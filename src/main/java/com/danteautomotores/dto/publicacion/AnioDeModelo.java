package com.danteautomotores.dto.publicacion;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Year;

/**
 * Año de modelo razonable: desde 1900 hasta el año siguiente al actual (los modelos del año próximo salen
 * a la venta antes). El tope se calcula al validar, por eso no alcanza con @Max. Acepta null (lo exige @NotNull).
 */
@Documented
@Constraint(validatedBy = AnioDeModelo.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AnioDeModelo {

    int MINIMO = 1900;

    String message() default "El año debe estar entre 1900 y el año próximo";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<AnioDeModelo, Integer> {
        @Override
        public boolean isValid(Integer anio, ConstraintValidatorContext context) {
            return anio == null || (anio >= MINIMO && anio <= Year.now().getValue() + 1);
        }
    }
}
