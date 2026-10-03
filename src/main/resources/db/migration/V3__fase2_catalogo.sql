-- Fase 2: datos nuevos para los filtros del catalogo publico (D-01 tipo, D-02 zona, D-03 precio anterior, D-04 fecha de venta).
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS tipo_carroceria varchar(255);
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS precio_anterior numeric(12,2);
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS fecha_vendido timestamp(6);
ALTER TABLE agencias ADD COLUMN IF NOT EXISTS zona varchar(255);

-- Los autos que ya figuraban como VENDIDO cuentan como vendidos desde esta migracion: sin fecha quedarian fuera del
-- catalogo sin haber tenido nunca sus 30 dias de visibilidad.
UPDATE publicaciones SET fecha_vendido = LOCALTIMESTAMP WHERE estado = 'VENDIDO' AND fecha_vendido IS NULL;

CREATE INDEX IF NOT EXISTS idx_publicaciones_agencia ON publicaciones (agencia_id);
CREATE INDEX IF NOT EXISTS idx_publicaciones_estado_fecha ON publicaciones (estado, fecha_publicacion DESC);
CREATE INDEX IF NOT EXISTS idx_fotos_publicacion_orden ON fotos_publicacion (publicacion_id, orden);
CREATE INDEX IF NOT EXISTS idx_favoritos_publicacion ON favoritos (publicacion_id);
CREATE INDEX IF NOT EXISTS idx_consultas_publicacion ON consultas (publicacion_id);
