package br.com.fiap.farmasusdigital.domain.service;

/**
 * Validacao do digito verificador do CPF (algoritmo mod11), sem nenhuma
 * dependencia de framework.
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    public static boolean isValido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String digitos = cpf.replaceAll("[^0-9]", "");
        if (digitos.length() != 11 || todosDigitosIguais(digitos)) {
            return false;
        }
        int primeiroDigito = calcularDigitoVerificador(digitos.substring(0, 9), 10);
        int segundoDigito = calcularDigitoVerificador(digitos.substring(0, 9) + primeiroDigito, 11);
        return digitos.equals(digitos.substring(0, 9) + primeiroDigito + segundoDigito);
    }

    private static boolean todosDigitosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

    private static int calcularDigitoVerificador(String base, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (char c : base.toCharArray()) {
            soma += Character.getNumericValue(c) * peso;
            peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public static String somenteDigitos(String cpf) {
        return cpf == null ? null : cpf.replaceAll("[^0-9]", "");
    }
}
