package com.danteautomotores.dto.usuario;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.enums.DatoFaltante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

/**
 * Ficha de un usuario para la agencia: con quién se está hablando (MSG-09). Es, junto con {@link UsuarioResponse}
 * (el perfil propio), el único DTO que lleva el DNI y el teléfono, y solo lo ve la cuenta admin (Ley 25.326, D-11).
 * El mail, el teléfono y el DNI quedan fuera del toString, y también el historial, cuyas filas repiten el mail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioFichaResponse {
    private Long id;
    private String nombre;
    private String apellido;
    @ToString.Exclude
    private String email;
    @ToString.Exclude
    private String telefono;
    @ToString.Exclude
    private String dni;
    private boolean emailConfirmado;
    private boolean cuentaVerificada;
    private List<DatoFaltante> faltantes;
    // Fecha sin hora: alcanza para "cliente desde".
    private LocalDate fechaRegistro;
    // Las conversaciones del usuario vistas por la agencia, la de mensaje más reciente primero. Las cotizaciones se
    // suman a la ficha con la Fase 5 (D-15).
    @ToString.Exclude
    private List<ConversacionResumenResponse> conversaciones;
}
