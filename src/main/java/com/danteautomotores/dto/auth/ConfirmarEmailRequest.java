package com.danteautomotores.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** Token del link de confirmación de mail. Fuera del toString: quien lo lea de un log podría confirmar la cuenta. */
@Data
public class ConfirmarEmailRequest {

    @NotBlank
    @Size(max = 200)
    @ToString.Exclude
    private String token;
}
