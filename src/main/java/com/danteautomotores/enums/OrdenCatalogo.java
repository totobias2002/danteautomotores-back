package com.danteautomotores.enums;

/**
 * Criterios de orden del catálogo público. El cliente solo puede elegir uno de estos (nunca una propiedad interna
 * de la entidad), y un valor desconocido cae a RELEVANCIA para que un link viejo no rompa la página.
 */
public enum OrdenCatalogo {
    RELEVANCIA,
    PRECIO_ASC,
    PRECIO_DESC,
    ANIO_DESC,
    KM_ASC;

    public static OrdenCatalogo desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return RELEVANCIA;
        }
        for (OrdenCatalogo orden : values()) {
            if (orden.name().equalsIgnoreCase(valor.trim())) {
                return orden;
            }
        }
        return RELEVANCIA;
    }
}
