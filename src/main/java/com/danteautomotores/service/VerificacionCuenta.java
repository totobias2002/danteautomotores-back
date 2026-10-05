package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.service.identidad.NormalizadorDeContacto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla única de cuenta verificada (D-01): nombre, apellido, teléfono y DNI cargados y el mail confirmado.
 * La obligatoriedad vive acá y no en la base porque las cuentas viejas tienen apellido y DNI nulos (D-09).
 * Un ADMIN nunca tiene faltantes: no compra ni cotiza, así que no se le pide completar nada.
 */
@Component
public class VerificacionCuenta {

    public List<DatoFaltante> faltantes(Usuario usuario) {
        List<DatoFaltante> faltantes = new ArrayList<>();
        if (usuario.getRol() == Rol.ADMIN) {
            return faltantes;
        }
        if (estaEnBlanco(usuario.getApellido())) {
            faltantes.add(DatoFaltante.APELLIDO);
        }
        // Un teléfono viejo de texto libre que no es un celular válido cuenta como faltante: se vuelve a cargar (D-09).
        if (estaEnBlanco(usuario.getTelefono()) || !NormalizadorDeContacto.esCelularValido(usuario.getTelefono())) {
            faltantes.add(DatoFaltante.TELEFONO);
        }
        if (estaEnBlanco(usuario.getDni())) {
            faltantes.add(DatoFaltante.DNI);
        }
        if (!usuario.isEmailConfirmado()) {
            faltantes.add(DatoFaltante.EMAIL_SIN_CONFIRMAR);
        }
        return faltantes;
    }

    public boolean estaVerificada(Usuario usuario) {
        return faltantes(usuario).isEmpty();
    }

    /** Corta la acción con {@link CuentaNoVerificadaException} si a la cuenta le falta algo; si no, no hace nada. */
    public void exigir(Usuario usuario) {
        List<DatoFaltante> faltantes = faltantes(usuario);
        if (!faltantes.isEmpty()) {
            throw new CuentaNoVerificadaException(faltantes);
        }
    }

    private static boolean estaEnBlanco(String texto) {
        return texto == null || texto.isBlank();
    }
}
