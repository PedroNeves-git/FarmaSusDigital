package br.com.fiap.farmasusdigital.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
class CancelarReservaUseCaseTest {

    @Mock
    private ReservaGateway reservaGateway;

    @Mock
    private MedicamentoGateway medicamentoGateway;

    private CancelarReservaUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clockFixo = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
        useCase = new CancelarReservaUseCase(reservaGateway, medicamentoGateway, clockFixo);
    }

    @Test
    void deveCancelarReservaEDevolverItensAoEstoque() {
        Reserva reserva = new Reserva(1L, 5L, java.util.List.of(new ItemReserva(1L, 10L, "Dipirona", 2)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);

        Medicamento medicamento = new Medicamento(10L, "Dipirona", 3);
        when(reservaGateway.buscarPorId(1L)).thenReturn(Optional.of(reserva));
        when(medicamentoGateway.buscarPorId(10L)).thenReturn(Optional.of(medicamento));
        when(reservaGateway.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva resultado = useCase.executar(1L);

        assertThat(resultado.getStatus()).isEqualTo(ReservaStatus.CANCELADA);
        assertThat(medicamento.getQuantidadeEstoque()).isEqualTo(5);
    }
}
