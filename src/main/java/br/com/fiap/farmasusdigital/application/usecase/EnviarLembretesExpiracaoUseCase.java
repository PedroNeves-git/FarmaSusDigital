package br.com.fiap.farmasusdigital.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.NotificacaoGateway;
import br.com.fiap.farmasusdigital.application.gateway.PacienteGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.model.ItemReserva;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

/**
 * Avisa proativamente o paciente quando sua reserva esta perto de expirar,
 * para reduzir o descarte por medicamento nao retirado a tempo.
 */
@Component
public class EnviarLembretesExpiracaoUseCase {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReservaGateway reservaGateway;
    private final PacienteGateway pacienteGateway;
    private final NotificacaoGateway notificacaoGateway;
    private final Clock clock;
    private final long antecedenciaHoras;

    public EnviarLembretesExpiracaoUseCase(ReservaGateway reservaGateway, PacienteGateway pacienteGateway,
                                            NotificacaoGateway notificacaoGateway, Clock clock,
                                            @Value("${reserva.lembrete.antecedencia-horas:1}") long antecedenciaHoras) {
        this.reservaGateway = reservaGateway;
        this.pacienteGateway = pacienteGateway;
        this.notificacaoGateway = notificacaoGateway;
        this.clock = clock;
        this.antecedenciaHoras = antecedenciaHoras;
    }

    public int executar() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime limiteAviso = agora.plusHours(antecedenciaHoras);

        List<Reserva> candidatas = reservaGateway.buscarAtivasSemLembreteVencendoAte(limiteAviso);

        int enviados = 0;
        for (Reserva reserva : candidatas) {
            if (!reserva.precisaDeLembrete(limiteAviso)) {
                continue;
            }
            pacienteGateway.buscarPorId(reserva.getPacienteId()).ifPresent(paciente ->
                    notificacaoGateway.enviarMensagem(paciente.getTelefone(), montarMensagem(reserva)));

            reserva.marcarLembreteEnviado();
            reservaGateway.salvar(reserva);
            enviados++;
        }
        return enviados;
    }

    private String montarMensagem(Reserva reserva) {
        String itens = reserva.getItens().stream()
                .map(ItemReserva::getNomeMedicamento)
                .collect(Collectors.joining(", "));
        return "Lembrete: sua reserva dos itens " + itens + " expira em "
                + reserva.getDataExpiracao().format(FORMATO_DATA)
                + ". Não esqueça de retirar no posto de saúde antes desse prazo!";
    }
}
