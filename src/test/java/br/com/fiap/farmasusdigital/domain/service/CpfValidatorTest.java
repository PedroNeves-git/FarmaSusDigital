package br.com.fiap.farmasusdigital.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class CpfValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725"})
    void deveAceitarCpfValidoComOuSemMascara(String cpf) {
        assertThat(CpfValidator.isValido(cpf)).isTrue();
    }

    @Test
    void deveRejeitarCpfComDigitoVerificadorInvalido() {
        assertThat(CpfValidator.isValido("529.982.247-26")).isFalse();
    }

    @Test
    void deveRejeitarCpfComTodosOsDigitosIguais() {
        assertThat(CpfValidator.isValido("111.111.111-11")).isFalse();
    }

    @Test
    void deveRejeitarCpfComTamanhoInvalido() {
        assertThat(CpfValidator.isValido("123")).isFalse();
    }

    @Test
    void deveRejeitarCpfNulo() {
        assertThat(CpfValidator.isValido(null)).isFalse();
    }
}
