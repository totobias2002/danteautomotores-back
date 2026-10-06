#!/usr/bin/env bash
# Levanta el back local contra una base de localhost (copia de otra, vacia o existente), corre un comando y lo apaga.
#
# Uso: bash scripts/verify/con-back-local.sh [--copia-de <origen> | --vacia] <base> <comando...>
#
#   --copia-de <origen>  crea <base> como copia de <origen> y la borra al terminar (aborta si <base> ya existe)
#   --vacia              crea <base> vacia y la borra al terminar (aborta si <base> ya existe)
#   (sin flag)           usa la base <base> que ya existe y NUNCA la borra
#
# Al comando se le exporta API=http://localhost:$PUERTO_BACK/api para que apunte al back recien levantado, y LOG_BACK con
# la ruta del log del back (los humos de cuentas leen de ahi los links de los mails de desarrollo).
# Variables: PUERTO_BACK (8080), PG_CONTENEDOR (danteautomotores-db), PG_PUERTO (5433), PG_USUARIO (dante),
#            PG_CLAVE (dante_dev_password), SALTAR_BUILD (1 = no recompila el jar).
# La base siempre es de localhost: el script no acepta otro host. Nunca crea ni borra la base "danteautomotores".
# Solo borra una base que creo en esta misma corrida: si la base pedida ya existe, se niega y no la toca.

set -u

PUERTO_BACK="${PUERTO_BACK:-8080}"
PG_CONTENEDOR="${PG_CONTENEDOR:-danteautomotores-db}"
PG_PUERTO="${PG_PUERTO:-5433}"
PG_USUARIO="${PG_USUARIO:-dante}"
PG_CLAVE="${PG_CLAVE:-dante_dev_password}"
SALTAR_BUILD="${SALTAR_BUILD:-0}"
BASE_DE_DESARROLLO="danteautomotores"

fallar() {
  echo "con-back-local: $*" >&2
  exit 1
}

# ---- Argumentos ----
MODO="existente"
ORIGEN=""
if [ "${1:-}" = "--copia-de" ]; then
  MODO="copia"
  ORIGEN="${2:-}"
  shift 2 2>/dev/null || fallar "falta el nombre de la base de origen despues de --copia-de"
elif [ "${1:-}" = "--vacia" ]; then
  MODO="vacia"
  shift
fi
BASE="${1:-}"
[ -n "$BASE" ] || fallar "uso: con-back-local.sh [--copia-de <origen> | --vacia] <base> <comando...>"
shift
[ "$#" -gt 0 ] || fallar "falta el comando a correr"

[[ "$BASE" =~ ^[a-z0-9_]+$ ]] || fallar "nombre de base invalido: $BASE (solo a-z, 0-9 y _)"
if [ "$MODO" = "copia" ]; then
  [[ "$ORIGEN" =~ ^[a-z0-9_]+$ ]] || fallar "nombre de base de origen invalido: $ORIGEN (solo a-z, 0-9 y _)"
  [ "$BASE" != "$ORIGEN" ] || fallar "la base de destino no puede ser la de origen"
fi
if [ "$MODO" != "existente" ] && [ "$BASE" = "$BASE_DE_DESARROLLO" ]; then
  fallar "me niego a crear o borrar la base $BASE_DE_DESARROLLO"
fi
if [ "$MODO" = "existente" ] && [ "$BASE" = "$BASE_DE_DESARROLLO" ]; then
  echo "con-back-local: ATENCION: el back va a migrar la base $BASE_DE_DESARROLLO (Flyway). Usa --copia-de para no tocarla." >&2
fi

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP="${TMPDIR:-/tmp}"
JAR_COPIA="$TMP/dante-back-$BASE.jar"
LOG="$TMP/dante-back-$BASE.log"
URL_BACK="http://localhost:$PUERTO_BACK"

# ---- Entorno de Java y Maven (solo hay JDK 17 en esta maquina) ----
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Java/jdk-17}"
for dir_maven in "/c/Users/toto/.maven/maven-3.9.16/bin" "/c/Program Files/Maven/apache-maven-3.9.16/bin"; do
  [ -d "$dir_maven" ] && export PATH="$dir_maven:$PATH" && break
done

# ---- Puerto libre ----
if curl -s -o /dev/null --max-time 2 "$URL_BACK/"; then
  fallar "puerto $PUERTO_BACK ocupado (algo ya responde en $URL_BACK); usa otro con PUERTO_BACK=<puerto>"
fi

psql_admin() {
  docker exec "$PG_CONTENEDOR" psql -U "$PG_USUARIO" -d postgres -v ON_ERROR_STOP=1 -q -c "$1"
}

psql_admin_valor() {
  docker exec "$PG_CONTENEDOR" psql -U "$PG_USUARIO" -d postgres -v ON_ERROR_STOP=1 -q -t -A -c "$1"
}

BASE_CREADA=0
PID_BACK=""

limpiar() {
  if [ -n "$PID_BACK" ]; then
    # En Git Bash el java es un proceso nativo de Windows: se lo detiene tambien por su pid de Windows.
    WINPID="$(cat "/proc/$PID_BACK/winpid" 2>/dev/null || true)"
    kill "$PID_BACK" 2>/dev/null || true
    if [ -n "$WINPID" ]; then
      taskkill //PID "$WINPID" //T //F >/dev/null 2>&1 || true
    fi
    wait "$PID_BACK" 2>/dev/null || true
    PID_BACK=""
  fi
  if [ "$BASE_CREADA" = "1" ]; then
    # Solo se borra lo que este script creo en esta corrida, y nunca la base de desarrollo.
    if [ "$BASE" != "$BASE_DE_DESARROLLO" ]; then
      psql_admin "DROP DATABASE IF EXISTS $BASE WITH (FORCE)" >/dev/null 2>&1 || true
    fi
    BASE_CREADA=0
  fi
  rm -f "$JAR_COPIA"
}
trap limpiar EXIT
trap 'exit 130' INT TERM

# ---- Base de datos ----
case "$MODO" in
  copia|vacia)
    # Nunca se borra una base que este script no creo en esta corrida: si ya existe (de otra persona, de otro uso o de una
    # corrida anterior cortada) se aborta antes de tocar nada. Para reutilizarla, borrarla a mano o usar otro nombre.
    EXISTE="$(psql_admin_valor "SELECT 1 FROM pg_database WHERE datname = '$BASE'")" \
      || fallar "no pude consultar si la base $BASE existe (contenedor $PG_CONTENEDOR arriba?)"
    [ -z "$EXISTE" ] \
      || fallar "la base $BASE ya existe y no la cree yo: no la borro. Usa otro nombre, o borrala a mano si es descartable (docker exec $PG_CONTENEDOR psql -U $PG_USUARIO -d postgres -c 'DROP DATABASE $BASE')"
    ;;
esac
case "$MODO" in
  copia)
    if psql_admin "CREATE DATABASE $BASE TEMPLATE $ORIGEN" 2>/dev/null; then
      BASE_CREADA=1
    else
      # TEMPLATE falla si hay conexiones abiertas al origen (por ejemplo, el back de desarrollo corriendo).
      echo "con-back-local: TEMPLATE no disponible (conexiones abiertas a $ORIGEN); copio con pg_dump | pg_restore"
      psql_admin "CREATE DATABASE $BASE" || fallar "no pude crear la base $BASE"
      BASE_CREADA=1
      docker exec "$PG_CONTENEDOR" sh -c "pg_dump -Fc -U '$PG_USUARIO' '$ORIGEN' | pg_restore -U '$PG_USUARIO' -d '$BASE' --no-owner" \
        || fallar "no pude copiar $ORIGEN a $BASE"
    fi
    ;;
  vacia)
    psql_admin "CREATE DATABASE $BASE" || fallar "no pude crear la base $BASE"
    BASE_CREADA=1
    ;;
esac

# ---- Jar ----
if [ "$SALTAR_BUILD" != "1" ]; then
  echo "con-back-local: compilando el jar..."
  mvn -B -o -q -Djava.version=17 -DskipTests -f "$REPO/pom.xml" package || fallar "fallo la compilacion"
fi
JAR_ORIGEN="$(ls "$REPO"/target/danteautomotores-back-*.jar 2>/dev/null | grep -v '\.original$' | head -n 1)"
[ -n "$JAR_ORIGEN" ] || fallar "no encuentro el jar en $REPO/target (corre sin SALTAR_BUILD)"
# Se corre una copia: en Windows un jar en uso queda bloqueado y rompe un mvn package posterior.
cp "$JAR_ORIGEN" "$JAR_COPIA"
JAR_JAVA="$JAR_COPIA"
command -v cygpath >/dev/null 2>&1 && JAR_JAVA="$(cygpath -m "$JAR_COPIA")"

# ---- Back ----
echo "con-back-local: levantando el back en $URL_BACK contra la base $BASE (log: $LOG)"
PORT="$PUERTO_BACK" \
SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:$PG_PUERTO/$BASE" \
SPRING_DATASOURCE_USERNAME="$PG_USUARIO" \
SPRING_DATASOURCE_PASSWORD="$PG_CLAVE" \
APP_CORS_ALLOWED_ORIGINS="http://localhost:5173, http://localhost:5174" \
  "$JAVA_HOME/bin/java" -jar "$JAR_JAVA" >"$LOG" 2>&1 &
PID_BACK=$!

LISTO=0
for _ in $(seq 1 120); do
  if ! kill -0 "$PID_BACK" 2>/dev/null; then
    break
  fi
  CODIGO="$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "$URL_BACK/api/agencias" || true)"
  if [ "$CODIGO" = "200" ]; then
    LISTO=1
    break
  fi
  sleep 1
done

if [ "$LISTO" != "1" ]; then
  echo "con-back-local: el back no quedo listo (murio o se agoto el tiempo de 120 s). Ultimas 80 lineas del log:" >&2
  tail -n 80 "$LOG" >&2
  exit 1
fi

echo "con-back-local: lineas de Flyway:"
grep -iE "flyway|baselined|Migrating|migration|up to date" "$LOG" | sed 's/^/  /' || true

# ---- Comando ----
export API="$URL_BACK/api"
# Ruta del log del back para los humos que leen los mails (en desarrollo el back los escribe en el log). Node nativo de
# Windows no entiende las rutas /tmp de Git Bash: se convierte con cygpath.
LOG_BACK="$LOG"
command -v cygpath >/dev/null 2>&1 && LOG_BACK="$(cygpath -m "$LOG")"
export LOG_BACK
"$@"
CODIGO_COMANDO=$?
exit "$CODIGO_COMANDO"
