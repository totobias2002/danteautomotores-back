-- Cambios de la Fase 1. IF NOT EXISTS porque la base local de desarrollo ya los tiene (los agrego ddl-auto=update).
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS destacado boolean NOT NULL DEFAULT false;
ALTER TABLE fotos_publicacion ADD COLUMN IF NOT EXISTS public_id varchar(255);
