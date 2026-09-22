package br.com.fiap.farmasusdigital.domain.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class NomeFormatter {

    private NomeFormatter() {
    }

    public static String capitalizar(String nome) {
        if (nome == null || nome.isBlank()) {
            return nome;
        }
        return Arrays.stream(nome.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .map(palavra -> Character.toUpperCase(palavra.charAt(0)) + palavra.substring(1))
                .collect(Collectors.joining(" "));
    }
}
