package br.com.fiap.farmasusdigital.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NomeFormatterTest {

    @Test
    void deveCapitalizarCadaPalavra() {
        assertThat(NomeFormatter.capitalizar("pedro henrique neves")).isEqualTo("Pedro Henrique Neves");
    }

    @Test
    void deveNormalizarNomeTodoEmMaiusculas() {
        assertThat(NomeFormatter.capitalizar("MARIA SILVA")).isEqualTo("Maria Silva");
    }

    @Test
    void deveIgnorarEspacosExtras() {
        assertThat(NomeFormatter.capitalizar("  joão   da silva  ")).isEqualTo("João Da Silva");
    }
}
