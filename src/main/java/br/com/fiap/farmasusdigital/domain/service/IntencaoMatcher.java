package br.com.fiap.farmasusdigital.domain.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconhece a intencao numa mensagem livre do paciente a partir de
 * palavras-chave, tolerando acentuacao e frases completas em vez de exigir
 * uma palavra exata.
 */
public final class IntencaoMatcher {

    private static final List<String> GATILHOS_FINALIZAR = List.of(
            "finaliz", "conclu", "encerr", "fechar", "so isso", "nada mais", "e isso");

    private static final List<String> GATILHOS_CANCELAR_TUDO = List.of(
            "cancel", "desist", "esquece", "deixa pra la", "nao quero mais nada",
            "remover a reserva", "remover minha reserva", "remover reserva",
            "remover tudo", "apagar a reserva", "apagar tudo");

    private static final List<String> GATILHOS_REMOVER_ITEM = List.of(
            "remover esse", "remover este", "remover essa", "remove esse", "remove este",
            "tirar esse", "tirar este", "tirar essa", "tira esse",
            "excluir esse", "excluir este", "excluir essa",
            "remover o ultimo", "tirar o ultimo", "remover item", "tirar item");

    private static final List<String> GATILHOS_POSITIVOS = List.of(
            "sim", "pode", "confirmo", "isso mesmo", "quero sim", "com certeza", "afirmativo");

    private static final List<String> GATILHOS_NEGATIVOS = List.of(
            "nao", "negativo", "deixa assim", "mantem");

    private static final List<String> GATILHOS_AJUDA = List.of(
            "ajuda", "menu", "comandos", "o que posso fazer", "como funciona", "nao entendi o que fazer");

    private static final List<String> GATILHOS_CONSULTA_ITENS = List.of(
            "o que eu ja reservei", "o que ja tenho", "meus itens", "ver reserva", "ver minha reserva",
            "consultar reserva", "mostrar reserva", "o que tenho ate agora", "quais itens", "o que eu coloquei");

    private static final List<String> GATILHOS_CORRECAO_CADASTRO = List.of(
            "corrigir", "errei", "cpf errado", "mudar cpf", "trocar cpf", "digitei errado", "coloquei errado");

    private IntencaoMatcher() {
    }

    public static boolean pareceAjuda(String texto) {
        return contemAlgumGatilho(texto, GATILHOS_AJUDA);
    }

    public static boolean pareceConsultaDeItens(String texto) {
        return contemAlgumGatilho(texto, GATILHOS_CONSULTA_ITENS);
    }

    public static boolean pareceCorrecaoDeCadastro(String texto) {
        return contemAlgumGatilho(texto, GATILHOS_CORRECAO_CADASTRO);
    }

    private static final Pattern PADRAO_QUANTIDADE = Pattern.compile("^\\s*(\\d+)\\s*x?\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Separa uma quantidade no inicio da mensagem do nome do medicamento
     * (ex.: "2 dipironas" -> quantidade 2, nome "dipironas"). Sem quantidade
     * explicita, assume 1.
     */
    public static TextoComQuantidade extrairQuantidade(String texto) {
        if (texto == null) {
            return new TextoComQuantidade(1, "");
        }
        Matcher matcher = PADRAO_QUANTIDADE.matcher(texto.trim());
        if (matcher.matches()) {
            int quantidade = Integer.parseInt(matcher.group(1));
            if (quantidade >= 1) {
                return new TextoComQuantidade(quantidade, matcher.group(2).trim());
            }
        }
        return new TextoComQuantidade(1, texto.trim());
    }

    public record TextoComQuantidade(int quantidade, String nomeRestante) {
    }

    public static boolean pareceCancelamentoTotal(String texto) {
        return contemAlgumGatilho(texto, GATILHOS_CANCELAR_TUDO);
    }

    public static boolean pareceRemocaoDeItem(String texto) {
        return contemAlgumGatilho(texto, GATILHOS_REMOVER_ITEM);
    }

    public static boolean pareceFinalizacao(String texto) {
        return contemAlgumGatilho(texto, GATILHOS_FINALIZAR);
    }

    /**
     * Retorna TRUE/FALSE quando a intencao de confirmacao (sim/nao) e clara,
     * ou null quando a mensagem e ambigua e deveria ser perguntada de novo.
     */
    public static Boolean interpretarConfirmacao(String texto) {
        String normalizado = normalizar(texto);
        boolean positivo = GATILHOS_POSITIVOS.stream().anyMatch(gatilho -> contemComoPalavra(normalizado, gatilho));
        boolean negativo = GATILHOS_NEGATIVOS.stream().anyMatch(gatilho -> contemComoPalavra(normalizado, gatilho));
        if (positivo && !negativo) {
            return Boolean.TRUE;
        }
        if (negativo && !positivo) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static boolean contemAlgumGatilho(String texto, List<String> gatilhos) {
        String normalizado = normalizar(texto);
        return gatilhos.stream().anyMatch(normalizado::contains);
    }

    /**
     * Como "contains", mas exige limite de palavra (evita que "sim" seja
     * encontrado dentro de "assim"). Usado so para sim/nao, cujos gatilhos
     * sao curtos; os demais usam prefixo de proposito para pegar conjugacoes.
     */
    private static boolean contemComoPalavra(String textoNormalizado, String gatilho) {
        return Pattern.compile("\\b" + Pattern.quote(gatilho) + "\\b").matcher(textoNormalizado).find();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT).trim();
    }
}
