package br.com.fiap.farmasusdigital.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Test;

class ReservaExpiracaoPolicyTest {

    @Test
    void deveExpirarNoDiaSeguinteQuandoCriadaEmDiaUtil() {
        LocalDateTime segundaFeira = LocalDateTime.of(2026, Month.SEPTEMBER, 7, 10, 0);

        LocalDateTime expiracao = ReservaExpiracaoPolicy.calcularDataExpiracao(segundaFeira);

        assertThat(expiracao).isEqualTo(LocalDateTime.of(2026, Month.SEPTEMBER, 8, 10, 0));
    }

    @Test
    void devePularFinalDeSemanaQuandoCriadaNaSextaFeira() {
        LocalDateTime sextaFeira = LocalDateTime.of(2026, Month.SEPTEMBER, 4, 15, 30);

        LocalDateTime expiracao = ReservaExpiracaoPolicy.calcularDataExpiracao(sextaFeira);

        assertThat(expiracao).isEqualTo(LocalDateTime.of(2026, Month.SEPTEMBER, 7, 15, 30));
    }

    @Test
    void devePularFinalDeSemanaQuandoCriadaNoSabado() {
        LocalDateTime sabado = LocalDateTime.of(2026, Month.SEPTEMBER, 5, 9, 0);

        LocalDateTime expiracao = ReservaExpiracaoPolicy.calcularDataExpiracao(sabado);

        assertThat(expiracao).isEqualTo(LocalDateTime.of(2026, Month.SEPTEMBER, 7, 9, 0));
    }
}
