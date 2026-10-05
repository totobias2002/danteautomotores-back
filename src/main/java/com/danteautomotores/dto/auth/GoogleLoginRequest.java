package com.danteautomotores.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** ID token que entrega Google Identity Services. Fuera del toString: es una credencial. */
@Data
public class GoogleLoginRequest {

    @NotBlank
    @Size(max = 4096)
    @ToString.Exclude
    private String credential;
}
