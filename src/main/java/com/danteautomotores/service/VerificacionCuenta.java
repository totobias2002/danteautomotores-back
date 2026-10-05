package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
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
        if (estaEnBlanco(usuario.getTelefono())) {
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

    private static boolean estaEnBlanco(String texto) {
        return texto == null || texto.isBlank();
    }
}
