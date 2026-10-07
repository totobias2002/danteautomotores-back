package com.danteautomotores.dto;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.NoLeidosResponse;
import com.danteautomotores.dto.conversacion.UsuarioDeConversacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.dto.usuario.UsuarioFichaResponse;
import com.danteautomotores.dto.usuario.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Privacidad de la mensajería (D-11, T-04-32, Ley 25.326): el DNI y el teléfono de una persona solo salen en el perfil
 * propio y en la ficha que ve el admin. Ninguna respuesta de mensajería los lleva, ni la contraseña, y los datos de
 * contacto no se imprimen con toString. JUnit solo, sin Spring.
 */
class PrivacidadMensajeriaTest {

    private static final Set<String> PROHIBIDOS_EN_MENSAJERIA =
            Set.of("dni", "telefono", "password", "passwordhash", "googlesub");

    private static final List<Class<?>> DTOS_DE_MENSAJERIA = List.of(
            ConversacionResumenResponse.class,
            ConversacionDetalleResponse.class,
            MensajeResponse.class,
            UsuarioDeConversacionResponse.class,
            NoLeidosResponse.class,
            PublicacionResumenResponse.class);

    private static List<Field> camposDeInstancia(Class<?> tipo) {
        List<Field> campos = new ArrayList<>();
        for (Field campo : tipo.getDeclaredFields()) {
            if (!Modifier.isStatic(campo.getModifiers()) && !campo.isSynthetic()) {
                campos.add(campo);
            }
        }
        return campos;
    }

    private static boolean declara(Class<?> tipo, String nombre) {
        return camposDeInstancia(tipo).stream().anyMatch(c -> c.getName().equalsIgnoreCase(nombre));
    }

    @Test
    void ningunaRespuestaDeMensajeriaDeclaraDniTelefonoNiContrasenia() {
        assertThat(DTOS_DE_MENSAJERIA).hasSize(6);
        for (Class<?> dto : DTOS_DE_MENSAJERIA) {
            List<String> sensibles = camposDeInstancia(dto).stream()
                    .map(c -> c.getName().toLowerCase())
                    .filter(PROHIBIDOS_EN_MENSAJERIA::contains)
                    .toList();
            assertThat(sensibles).as("campos sensibles en %s", dto.getSimpleName()).isEmpty();
        }
    }

    @Test
    void losDtosAnidadosDeLaMensajeriaTambienEstanLimpios() {
        // Un campo que es otro DTO del proyecto se recorre también: no se esconde un dni un nivel más abajo.
        for (Class<?> dto : DTOS_DE_MENSAJERIA) {
            for (Field campo : camposDeInstancia(dto)) {
                Class<?> tipo = campo.getType();
                if (tipo.getPackageName().startsWith("com.danteautomotores.dto") && !DTOS_DE_MENSAJERIA.contains(tipo)) {
                    assertThat(tipo.getSimpleName()).as("DTO anidado de %s no revisado", dto.getSimpleName()).isIn(
                            DTOS_DE_MENSAJERIA.stream().map(Class::getSimpleName).toList());
                }
            }
        }
    }

    @Test
    void entreLasRespuestasDelPaqueteDeUsuariosSoloElPerfilPropioYLaFichaDeclaranDniYTelefono() {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Response")));
        Set<String> conDatosPersonales = new TreeSet<>();
        int revisadas = 0;
        for (BeanDefinition definicion : escaner.findCandidateComponents("com.danteautomotores.dto.usuario")) {
            Class<?> tipo = clase(definicion.getBeanClassName());
            revisadas++;
            if (declara(tipo, "dni") || declara(tipo, "telefono")) {
                conDatosPersonales.add(tipo.getSimpleName());
            }
        }

        assertThat(revisadas).isGreaterThanOrEqualTo(2);
        assertThat(conDatosPersonales).containsExactlyInAnyOrder("UsuarioResponse", "UsuarioFichaResponse");
    }

    @Test
    void elToStringDeLaFichaArmadaConValoresReconocibleNoImprimeContactoNiHistorial() {
        ConversacionResumenResponse fila = ConversacionResumenResponse.builder()
                .id(50L).ultimoMensaje("TEXTO-PRIVADO-9911").ultimoMensajeEn(Instant.parse("2026-10-07T15:30:00Z"))
                .usuario(UsuarioDeConversacionResponse.builder().id(7L).nombre("Ana").email("ficha-9911@x.com").build())
                .build();
        UsuarioFichaResponse ficha = UsuarioFichaResponse.builder()
                .id(7L).nombre("Ana").apellido("Lopez").email("ficha-9911@x.com").telefono("+5491100009911")
                .dni("39119911").emailConfirmado(true).cuentaVerificada(true).fechaRegistro(LocalDate.of(2026, 3, 15))
                .conversaciones(List.of(fila)).build();

        String texto = ficha.toString();

        assertThat(texto).doesNotContain("39119911").doesNotContain("+5491100009911").doesNotContain("9911");
        assertThat(texto).contains("Ana").contains("Lopez");
    }

    @Test
    void elToStringDelPerfilPropioNoImprimeDniNiTelefono() {
        UsuarioResponse perfil = UsuarioResponse.builder().id(7L).nombre("Ana").telefono("+5491100009911").dni("39119911").build();

        assertThat(perfil.toString()).doesNotContain("39119911").doesNotContain("9911");
    }

    @Test
    void elToStringDeLosDtosDeMensajeriaNoImprimeTextosNiMails() {
        UsuarioDeConversacionResponse usuario = UsuarioDeConversacionResponse.builder()
                .id(7L).nombre("Ana").apellido("Lopez").email("mensajeria-5521@x.com").build();
        ConversacionResumenResponse resumen = ConversacionResumenResponse.builder()
                .id(50L).ultimoMensaje("MENSAJE-PRIVADO-5521").usuario(usuario).build();
        MensajeResponse mensaje = MensajeResponse.builder().id(1L).texto("MENSAJE-PRIVADO-5521").build();
        ConversacionDetalleResponse detalle = ConversacionDetalleResponse.builder()
                .conversacion(resumen).mensajes(List.of(mensaje)).build();

        assertThat(resumen.toString()).doesNotContain("5521");
        assertThat(mensaje.toString()).doesNotContain("5521");
        assertThat(usuario.toString()).doesNotContain("5521");
        assertThat(detalle.toString()).doesNotContain("5521");
    }

    private static Class<?> clase(String nombre) {
        try {
            return Class.forName(nombre);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se pudo cargar " + nombre, e);
        }
    }
}
