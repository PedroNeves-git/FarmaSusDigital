package br.com.fiap.farmasusdigital.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.farmasusdigital.application.gateway.NotificacaoGateway;
import br.com.fiap.farmasusdigital.application.gateway.PacienteGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.model.ItemReserva;
import br.com.fiap.farmasusdigital.domain.model.Paciente;
import br.com.fiap.farmasusdigital.domain.model.Reserva;
import br.com.fiap.farmasusdigital.domain.model.ReservaStatus;

@ExtendWith(MockitoExtension.class)
class EnviarLembretesExpiracaoUseCaseTest {

    @Mock
    private ReservaGateway reservaGateway;

    @Mock
    private PacienteGateway pacienteGateway;

    @Mock
    private NotificacaoGateway notificacaoGateway;

    private EnviarLembretesExpiracaoUseCase useCase;

    private final Clock clockFixo = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        useCase = new EnviarLembretesExpiracaoUseCase(reservaGateway, pacienteGateway, notificacaoGateway, clockFixo, 1L);
    }

    @Test
    void deveEnviarLembreteEMarcarComoEnviado() {
        Reserva reserva = new Reserva(1L, 10L, List.of(new ItemReserva(1L, 100L, "Dipirona", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(clockFixo).minusHours(23),
                LocalDateTime.now(clockFixo).plusMinutes(30), null);
        Paciente paciente = new Paciente(10L, "52998224725", "Maria Silva", "999999");

        when(reservaGateway.buscarAtivasSemLembreteVencendoAte(any())).thenReturn(List.of(reserva));
        when(pacienteGateway.buscarPorId(10L)).thenReturn(Optional.of(paciente));
        when(reservaGateway.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int enviados = useCase.executar();

        assertThat(enviados).isEqualTo(1);
        assertThat(reserva.isLembreteEnviado()).isTrue();
        verify(notificacaoGateway).enviarMensagem(eq("999999"), anyString());
    }

    @Test
    void naoDeveEnviarNadaQuandoNaoHaReservasProximasDoVencimento() {
        when(reservaGateway.buscarAtivasSemLembreteVencendoAte(any())).thenReturn(List.of());

        int enviados = useCase.executar();

        assertThat(enviados).isZero();
        verify(notificacaoGateway, never()).enviarMensagem(anyString(), anyString());
    }
}
