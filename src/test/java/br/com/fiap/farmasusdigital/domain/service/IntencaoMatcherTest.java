package br.com.fiap.farmasusdigital.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class IntencaoMatcherTest {

    @ParameterizedTest
    @ValueSource(strings = {"finalizar", "desejo finalizar a reserva", "quero concluir", "pode encerrar",
            "quero fechar a reserva", "só isso mesmo", "e isso, obrigado", "pode confirmar",
            "confirmar reserva"})
    void deveReconhecerIntencaoDeFinalizar(String texto) {
        assertThat(IntencaoMatcher.pareceFinalizacao(texto)).isTrue();
    }

    @Test
    void naoDeveReconhecerNomeDeMedicamentoComoFinalizar() {
        assertThat(IntencaoMatcher.pareceFinalizacao("dipirona")).isFalse();
        assertThat(IntencaoMatcher.pareceFinalizacao("metformina 850mg")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelar", "quero cancelar tudo", "desisti", "ah esquece",
            "deixa pra lá", "não quero mais nada", "remover a reserva", "remover tudo"})
    void deveReconhecerIntencaoDeCancelamentoTotal(String texto) {
        assertThat(IntencaoMatcher.pareceCancelamentoTotal(texto)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"remover esse", "gostaria de remover esse da reserva", "tira esse",
            "quero excluir esse", "remover o último", "tirar item"})
    void deveReconhecerIntencaoDeRemoverUltimoItem(String texto) {
        assertThat(IntencaoMatcher.pareceRemocaoDeItem(texto)).isTrue();
    }

    @Test
    void naoDeveConfundirRemocaoDeItemComCancelamentoTotal() {
        assertThat(IntencaoMatcher.pareceCancelamentoTotal("gostaria de remover esse da reserva")).isFalse();
        assertThat(IntencaoMatcher.pareceRemocaoDeItem("quero cancelar tudo")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sim", "sim, pode", "confirmo", "com certeza"})
    void deveInterpretarConfirmacaoPositiva(String texto) {
        assertThat(IntencaoMatcher.interpretarConfirmacao(texto)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"não", "nao, deixa assim", "negativo"})
    void deveInterpretarConfirmacaoNegativa(String texto) {
        assertThat(IntencaoMatcher.interpretarConfirmacao(texto)).isFalse();
    }

    @Test
    void deveRetornarNuloQuandoConfirmacaoForAmbigua() {
        assertThat(IntencaoMatcher.interpretarConfirmacao("talvez")).isNull();
        assertThat(IntencaoMatcher.interpretarConfirmacao("")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ajuda", "menu", "comandos", "como funciona", "não entendi o que fazer"})
    void deveReconhecerIntencaoDeAjuda(String texto) {
        assertThat(IntencaoMatcher.pareceAjuda(texto)).isTrue();
    }

    @Test
    void naoDeveConfundirNomeDeMedicamentoComPedidoDeAjuda() {
        assertThat(IntencaoMatcher.pareceAjuda("dipirona")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"o que eu já reservei?", "o que já tenho", "meus itens", "ver minha reserva",
            "quais itens eu coloquei"})
    void deveReconhecerIntencaoDeConsultarItens(String texto) {
        assertThat(IntencaoMatcher.pareceConsultaDeItens(texto)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"corrigir", "errei o cpf", "cpf errado", "quero trocar cpf", "digitei errado"})
    void deveReconhecerIntencaoDeCorrigirCadastro(String texto) {
        assertThat(IntencaoMatcher.pareceCorrecaoDeCadastro(texto)).isTrue();
    }

    @Test
    void deveExtrairQuantidadeENomeQuandoMensagemComecaComNumero() {
        IntencaoMatcher.TextoComQuantidade resultado = IntencaoMatcher.extrairQuantidade("2 dipironas");

        assertThat(resultado.quantidade()).isEqualTo(2);
        assertThat(resultado.nomeRestante()).isEqualTo("dipironas");
    }

    @Test
    void deveAceitarXEntreQuantidadeENome() {
        IntencaoMatcher.TextoComQuantidade resultado = IntencaoMatcher.extrairQuantidade("3x paracetamol");

        assertThat(resultado.quantidade()).isEqualTo(3);
        assertThat(resultado.nomeRestante()).isEqualTo("paracetamol");
    }

    @Test
    void deveAssumirQuantidadeUmQuandoNaoHaNumeroNaMensagem() {
        IntencaoMatcher.TextoComQuantidade resultado = IntencaoMatcher.extrairQuantidade("dipirona");

        assertThat(resultado.quantidade()).isEqualTo(1);
        assertThat(resultado.nomeRestante()).isEqualTo("dipirona");
    }

    @Test
    void naoDeveEstourarQuandoNumeroInformadoForGrandeDemaisParaUmInt() {
        IntencaoMatcher.TextoComQuantidade resultado = IntencaoMatcher.extrairQuantidade("99999999999999999999 dipirona");

        assertThat(resultado.quantidade()).isEqualTo(1);
        assertThat(resultado.nomeRestante()).isEqualTo("99999999999999999999 dipirona");
    }
}
