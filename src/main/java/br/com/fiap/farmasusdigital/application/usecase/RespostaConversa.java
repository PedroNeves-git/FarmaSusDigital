package br.com.fiap.farmasusdigital.application.usecase;

import java.util.List;

/**
 * Resposta do bot a uma mensagem do paciente. Alem do texto, carrega
 * sugestoes de resposta rapida (ex.: "1", "2", "Sim", "Não") para canais que
 * suportem botoes de atalho; um canal que nao suporte pode simplesmente
 * ignorar as opcoes e mostrar so o texto.
 */
public record RespostaConversa(String mensagem, List<String> opcoesRapidas) {

    public static RespostaConversa semOpcoes(String mensagem) {
        return new RespostaConversa(mensagem, List.of());
    }

    public static RespostaConversa comOpcoes(String mensagem, List<String> opcoesRapidas) {
        return new RespostaConversa(mensagem, opcoesRapidas);
    }
}
