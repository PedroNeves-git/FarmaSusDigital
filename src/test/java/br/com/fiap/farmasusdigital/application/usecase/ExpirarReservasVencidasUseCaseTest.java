package br.com.fiap.farmasusdigital.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.model.ItemReserva;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.domain.model.Reserva;
import br.com.fiap.farmasusdigital.domain.model.ReservaStatus;

@ExtendWith(MockitoExtension.class)
class ExpirarReservasVencidasUseCaseTest {

    @Mock
    private ReservaGateway reservaGateway;

    @Mock
    private MedicamentoGateway medicamentoGateway;

    private ExpirarReservasVencidasUseCase useCase;

    private final Clock clockFixo = Clock.fixed(Instant.parse("2026-09-02T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        useCase = new ExpirarReservasVencidasUseCase(reservaGateway, medicamentoGateway, clockFixo);
    }

    @Test
    void deveExpirarReservasVencidasEDevolverEstoque() {
        Reserva reservaVencida = new Reserva(1L, 5L, List.of(new ItemReserva(1L, 10L, "Dipirona", 2)),
                ReservaStatus.ATIVA, LocalDateTime.now(clockFixo).minusDays(2),
                LocalDateTime.now(clockFixo).minusHours(1), null);

        Medicamento medicamento = new Medicamento(10L, "Dipirona", 3);
        when(reservaGateway.buscarAtivasVencidas(any())).thenReturn(List.of(reservaVencida));
        when(medicamentoGateway.buscarPorId(10L)).thenReturn(Optional.of(medicamento));
        when(reservaGateway.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int quantidade = useCase.executar();

        assertThat(quantidade).isEqualTo(1);
        assertThat(reservaVencida.getStatus()).isEqualTo(ReservaStatus.EXPIRADA);
        assertThat(medicamento.getQuantidadeEstoque()).isEqualTo(5);
    }

    @Test
    void naoDeveFazerNadaQuandoNaoHaReservasVencidas() {
        when(reservaGateway.buscarAtivasVencidas(any())).thenReturn(List.of());

        int quantidade = useCase.executar();

        assertThat(quantidade).isZero();
    }
}
