package br.com.fiap.farmasusdigital.domain.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Compara o nome de um medicamento com o texto que o paciente digitou no
 * bot, tolerando acentuacao diferente, caixa diferente e espacamento/ordem
 * das palavras diferente (ex.: "dipirona 500" deve casar com
 * "Dipirona Sodica 500mg").
 */
public final class MedicamentoNomeMatcher {

    private MedicamentoNomeMatcher() {
    }

    public static boolean correspondeA(String nomeMedicamento, String textoBusca) {
        if (nomeMedicamento == null || textoBusca == null || textoBusca.isBlank()) {
            return false;
        }
        String nomeNormalizado = normalizar(nomeMedicamento);
        String[] termos = normalizar(textoBusca).split("\\s+");

        for (String termo : termos) {
            if (!termo.isBlank() && !casaComOTermo(nomeNormalizado, termo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Alem da correspondencia exata, tolera o plural em portugues (ex.: o
     * paciente digitar "dipironas" deve encontrar "Dipirona Sodica 500mg").
     */
    private static boolean casaComOTermo(String nomeNormalizado, String termo) {
        if (nomeNormalizado.contains(termo)) {
            return true;
        }
        if (termo.length() > 3 && termo.endsWith("s")) {
            return nomeNormalizado.contains(termo.substring(0, termo.length() - 1));
        }
        return false;
    }

    private static String normalizar(String texto) {
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT).trim();
    }
}
