package br.com.fiap.farmasusdigital.domain.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * Regra de negocio isolada de framework: uma reserva expira apos 1 dia util
 * contado a partir da criacao, pulando sabados e domingos.
 */
public final class ReservaExpiracaoPolicy {

    private ReservaExpiracaoPolicy() {
    }

    public static LocalDateTime calcularDataExpiracao(LocalDateTime dataCriacao) {
        return somarDiasUteis(dataCriacao, 1);
    }

    private static LocalDateTime somarDiasUteis(LocalDateTime data, int diasUteis) {
        LocalDateTime resultado = data;
        int adicionados = 0;
        while (adicionados < diasUteis) {
            resultado = resultado.plusDays(1);
            if (!isFimDeSemana(resultado)) {
                adicionados++;
            }
        }
        return resultado;
    }

    private static boolean isFimDeSemana(LocalDateTime data) {
        DayOfWeek diaDaSemana = data.getDayOfWeek();
        return diaDaSemana == DayOfWeek.SATURDAY || diaDaSemana == DayOfWeek.SUNDAY;
    }
}
