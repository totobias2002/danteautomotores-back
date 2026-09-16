package com.danteautomotores.dto.consulta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaResponse {
    private Long id;
    private Long publicacionId;
    private String nombreComprador;
    private String emailComprador;
    private String telefonoComprador;
    private String mensaje;
    private LocalDateTime fecha;
}
