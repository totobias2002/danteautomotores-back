-- Fase 4: el comprador puede borrar de su lista una conversacion cerrada. Es un ocultamiento solo de su lado: la
-- conversacion y todos sus mensajes siguen en la base y la agencia los ve siempre (bandeja y ficha del usuario).
-- Es aditiva: una columna nueva con valor por defecto, asi que la version anterior del back sigue funcionando.

ALTER TABLE conversaciones
    ADD COLUMN oculta_para_usuario boolean NOT NULL DEFAULT false;
