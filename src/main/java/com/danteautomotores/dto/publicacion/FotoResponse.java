package com.danteautomotores.dto.publicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotoResponse {
    private Long id;
    private String url;
    private Integer orden;
}
