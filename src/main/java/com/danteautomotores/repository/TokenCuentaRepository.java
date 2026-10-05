package com.danteautomotores.repository;

import com.danteautomotores.entity.TokenCuenta;
import com.danteautomotores.enums.TipoTokenCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenCuentaRepository extends JpaRepository<TokenCuenta, Long> {

    Optional<TokenCuenta> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TokenCuenta t where t.usuarioId = :usuarioId and t.tipo = :tipo")
    int deleteByUsuarioIdAndTipo(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoTokenCuenta tipo);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TokenCuenta t where t.usuarioId = :usuarioId")
    int deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Consumo atómico: un UPDATE condicional que solo gana un pedido aunque lleguen dos a la vez (el segundo espera
     * el commit del primero y ya no encuentra el token sin usar). Devuelve 1 para quien gana y 0 si el token no
     * existe, ya se usó o venció.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenCuenta t set t.usadoEn = :ahora where t.tokenHash = :hash and t.tipo = :tipo "
            + "and t.usadoEn is null and t.expiraEn > :ahora")
    int consumir(@Param("hash") String hash, @Param("tipo") TipoTokenCuenta tipo, @Param("ahora") LocalDateTime ahora);
}
