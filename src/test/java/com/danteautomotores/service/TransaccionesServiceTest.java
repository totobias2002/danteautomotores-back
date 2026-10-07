package com.danteautomotores.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Con spring.jpa.open-in-view=false los mappers (que leen agencia, fotos y publicación lazy) solo funcionan dentro
 * de una transacción. Guarda que cada método público de los services que mapean entidades la tenga, y que
 * open-in-view siga apagado.
 */
class TransaccionesServiceTest {

    @Test
    void openInViewEstaApagado() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties propiedades = yaml.getObject();

        assertThat(propiedades).isNotNull();
        assertThat(propiedades.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void todosLosMetodosPublicosDeLosServicesQueMapeanEntidadesSonTransaccionales() {
        for (Class<?> service : List.of(PublicacionService.class, FavoritoService.class,
                ConversacionService.class, ConversacionAdminService.class, UsuarioAdminService.class)) {
            List<Method> sinTransaccion = Arrays.stream(service.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                    .filter(m -> AnnotatedElementUtils.findMergedAnnotation(m, Transactional.class) == null
                            && AnnotatedElementUtils.findMergedAnnotation(service, Transactional.class) == null)
                    .toList();

            assertThat(sinTransaccion).as("métodos sin @Transactional en %s", service.getSimpleName()).isEmpty();
        }
    }

    @Test
    void lasLecturasDePublicacionSonReadOnly() throws Exception {
        for (String nombre : List.of("obtenerPorId", "listarParaAdmin", "obtenerImpactoEliminacion")) {
            Method metodo = Arrays.stream(PublicacionService.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(nombre)).findFirst().orElseThrow();
            Transactional transaccional = AnnotatedElementUtils.findMergedAnnotation(metodo, Transactional.class);

            assertThat(transaccional).as(nombre).isNotNull();
            assertThat(transaccional.readOnly()).as(nombre).isTrue();
        }
    }
}
