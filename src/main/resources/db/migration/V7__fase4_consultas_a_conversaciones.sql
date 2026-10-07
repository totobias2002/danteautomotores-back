-- Fase 4: las consultas viejas pasan a la bandeja de conversaciones (D-01, D-13).
-- Es aditiva: solo inserta conversaciones y mensajes nuevos y cambia la clave foranea de consultas hacia
-- publicaciones. No borra ni modifica ninguna fila existente y no se edita V1 a V6. La tabla consultas queda
-- inerte pero intacta (tambien las consultas que no coinciden con ninguna cuenta y las de cuentas admin), de modo
-- que la version anterior del back sigue funcionando contra esta base.
-- Las fechas de consultas son timestamp sin zona del servidor (UTC en produccion), igual que las columnas nuevas.

-- 1. Una conversacion de COMPRA abierta por cada par (cuenta de comprador, auto) que aparezca en consultas.
--    La cuenta se toma por el mail sin distinguir mayusculas y solo si es COMPRADOR. Una fecha nula se reemplaza
--    por la hora actual en UTC. ON CONFLICT respeta el indice unico parcial de V6 (una abierta por usuario y auto).
INSERT INTO conversaciones (tipo, estado, usuario_id, publicacion_id, creada_en, ultimo_mensaje_en)
SELECT 'COMPRA',
       'ABIERTA',
       u.id,
       c.publicacion_id,
       min(coalesce(c.fecha, now() AT TIME ZONE 'UTC')),
       max(coalesce(c.fecha, now() AT TIME ZONE 'UTC'))
FROM consultas c
         JOIN usuarios u ON lower(u.email) = lower(c.email_comprador) AND u.rol = 'COMPRADOR'
GROUP BY u.id, c.publicacion_id
ON CONFLICT (usuario_id, publicacion_id) WHERE tipo = 'COMPRA' AND estado = 'ABIERTA' DO NOTHING;

-- 2. Un mensaje del usuario por cada consulta migrada, en el orden del id de la consulta, con su fecha original y
--    sin leer por la agencia (nadie la vio nunca). El texto cumple el CHECK de V6: sin espacios ni saltos de linea
--    en los bordes, de hasta 2000 caracteres, o un texto fijo si queda vacio. Solo se cargan en conversaciones
--    que todavia no tienen mensajes (las que acaba de crear el paso 1).
INSERT INTO mensajes (conversacion_id, autor_id, autor_tipo, texto, creado_en, leido_en)
SELECT conv.id,
       u.id,
       'USUARIO',
       CASE
           WHEN char_length(btrim(coalesce(c.mensaje, ''), E' \t\r\n')) = 0 THEN 'Consulta sin mensaje'
           ELSE left(btrim(c.mensaje, E' \t\r\n'), 2000)
       END,
       coalesce(c.fecha, now() AT TIME ZONE 'UTC'),
       NULL
FROM consultas c
         JOIN usuarios u ON lower(u.email) = lower(c.email_comprador) AND u.rol = 'COMPRADOR'
         JOIN conversaciones conv ON conv.usuario_id = u.id
                                 AND conv.publicacion_id = c.publicacion_id
                                 AND conv.tipo = 'COMPRA'
                                 AND conv.estado = 'ABIERTA'
WHERE NOT EXISTS (SELECT 1 FROM mensajes m WHERE m.conversacion_id = conv.id)
ORDER BY c.id;

-- 3. Borrar un auto no puede quedar trabado por una consulta vieja que nadie vuelve a leer: la clave foranea de
--    consultas hacia publicaciones pasa a ON DELETE CASCADE. El nombre que le puso Hibernate en produccion difiere
--    del de otras bases, asi que se busca por el catalogo.
DO
$$
DECLARE
    restriccion text;
BEGIN
    FOR restriccion IN
        SELECT con.conname
        FROM pg_constraint con
        WHERE con.conrelid = 'public.consultas'::regclass
          AND con.confrelid = 'public.publicaciones'::regclass
          AND con.contype = 'f'
        LOOP
            EXECUTE format('ALTER TABLE public.consultas DROP CONSTRAINT %I', restriccion);
        END LOOP;

    ALTER TABLE public.consultas
        ADD CONSTRAINT fk_consultas_publicacion FOREIGN KEY (publicacion_id)
            REFERENCES public.publicaciones (id) ON DELETE CASCADE;
END
$$;
