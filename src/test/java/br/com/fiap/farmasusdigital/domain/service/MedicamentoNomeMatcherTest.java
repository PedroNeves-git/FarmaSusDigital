package br.com.fiap.farmasusdigital.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MedicamentoNomeMatcherTest {

    @Test
    void deveCorresponderQuandoNomeEIgual() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "Dipirona Sódica 500mg")).isTrue();
    }

    @Test
    void deveCorresponderComApenasParteDoNome() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "dipirona")).isTrue();
    }

    @Test
    void deveCorresponderIgnorandoAcentuacao() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "sodica")).isTrue();
    }

    @Test
    void deveCorresponderComPalavrasEmOrdemDiferente() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "500 dipirona")).isTrue();
    }

    @Test
    void deveCorresponderComEspacamentoExtra() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "  dipirona   500  ")).isTrue();
    }

    @Test
    void naoDeveCorresponderQuandoAlgumTermoNaoAparece() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "dipirona 750")).isFalse();
    }

    @Test
    void naoDeveCorresponderComTextoVazio() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "   ")).isFalse();
    }

    @Test
    void deveCorresponderComPluralDoTermoDeBusca() {
        assertThat(MedicamentoNomeMatcher.correspondeA("Dipirona Sódica 500mg", "dipironas")).isTrue();
    }
}
