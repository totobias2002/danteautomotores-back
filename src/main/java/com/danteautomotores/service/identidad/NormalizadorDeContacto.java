package com.danteautomotores.service.identidad;

import com.danteautomotores.exception.ReglaDeNegocioException;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.regex.Pattern;

/**
 * Normalización y validación del celular argentino (D-03) y del DNI (D-05). Puras y sin Spring.
 * <p>
 * Limitación conocida: libphonenumber no distingue un fijo de un celular cuando el usuario no escribe el 15 o el 9.
 * D-03 pide solo "celular argentino válido (código de área + número)", así que se acepta cualquier número argentino
 * válido y se guarda como celular. El 9 se agrega siempre porque el campo es "celular" y para que
 * {@code wa.me/549...} funcione en las Fases 4 y 5.
 * <p>
 * El DNI se valida solo por formato: sin RENAPER ni foto, el admin confirma la identidad en persona.
 */
public final class NormalizadorDeContacto {

    public static final String MENSAJE_CELULAR_INVALIDO =
            "Ingresá un celular argentino válido, con código de área. Ej: 11 2345-6789";
    public static final String MENSAJE_DNI_INVALIDO = "El DNI debe tener 7 u 8 dígitos, sin puntos.";

    private static final String REGION = "AR";
    private static final int CODIGO_PAIS_ARGENTINA = 54;
    private static final int DIGITOS_NACIONALES = 10;
    private static final Pattern SEPARADORES_DE_DNI = Pattern.compile("[.\\s-]");
    private static final Pattern DNI = Pattern.compile("[1-9]\\d{6,7}");

    private static final PhoneNumberUtil TELEFONOS = PhoneNumberUtil.getInstance();

    private NormalizadorDeContacto() {
    }

    /** Devuelve "+549" más diez dígitos o lanza {@link ReglaDeNegocioException} con el mensaje para el usuario. */
    public static String normalizarCelular(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new ReglaDeNegocioException(MENSAJE_CELULAR_INVALIDO);
        }
        try {
            PhoneNumber numero = TELEFONOS.parse(entrada.trim(), REGION);
            if (numero.getCountryCode() != CODIGO_PAIS_ARGENTINA || !TELEFONOS.isValidNumberForRegion(numero, REGION)) {
                throw new ReglaDeNegocioException(MENSAJE_CELULAR_INVALIDO);
            }
            String nacional = TELEFONOS.getNationalSignificantNumber(numero);
            if (nacional.length() == DIGITOS_NACIONALES + 1 && nacional.startsWith("9")) {
                nacional = nacional.substring(1);
            }
            if (nacional.length() != DIGITOS_NACIONALES) {
                throw new ReglaDeNegocioException(MENSAJE_CELULAR_INVALIDO);
            }
            return "+549" + nacional;
        } catch (NumberParseException e) {
            throw new ReglaDeNegocioException(MENSAJE_CELULAR_INVALIDO);
        }
    }

    /** Igual que {@link #normalizarCelular} pero devuelve un boolean y nunca lanza. */
    public static boolean esCelularValido(String entrada) {
        try {
            normalizarCelular(entrada);
            return true;
        } catch (ReglaDeNegocioException e) {
            return false;
        }
    }

    /** Devuelve el DNI sin puntos, espacios ni guiones (7 u 8 dígitos, sin cero inicial) o lanza la regla. */
    public static String normalizarDni(String entrada) {
        if (entrada == null) {
            throw new ReglaDeNegocioException(MENSAJE_DNI_INVALIDO);
        }
        String limpio = SEPARADORES_DE_DNI.matcher(entrada.trim()).replaceAll("");
        if (!DNI.matcher(limpio).matches()) {
            throw new ReglaDeNegocioException(MENSAJE_DNI_INVALIDO);
        }
        return limpio;
    }
}
