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
class RemoverUltimoItemReservaUseCaseTest {

    @Mock
    private ReservaGateway reservaGateway;

    @Mock
    private MedicamentoGateway medicamentoGateway;

    private RemoverUltimoItemReservaUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clockFixo = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
        useCase = new RemoverUltimoItemReservaUseCase(reservaGateway, medicamentoGateway, clockFixo);
    }

    @Test
    void deveRemoverUltimoItemEDevolverEstoqueQuandoRestamOutrosItens() {
        Reserva reserva = new Reserva(50L, 1L,
                List.of(new ItemReserva(1L, 10L, "Losartana 50mg", 1), new ItemReserva(2L, 20L, "Dipirona", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        Medicamento dipirona = new Medicamento(20L, "Dipirona", 4);

        when(reservaGateway.buscarPorId(50L)).thenReturn(Optional.of(reserva));
        when(medicamentoGateway.buscarPorId(20L)).thenReturn(Optional.of(dipirona));
        when(reservaGateway.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RemoverUltimoItemReservaUseCase.Resultado resultado = useCase.executar(50L);

        assertThat(resultado.itemRemovido().getNomeMedicamento()).isEqualTo("Dipirona");
        assertThat(resultado.reservaAtualizada()).isPresent();
        assertThat(resultado.reservaAtualizada().get().getItens()).hasSize(1);
        assertThat(dipirona.getQuantidadeEstoque()).isEqualTo(5);
    }

    @Test
    void deveCancelarReservaQuandoRemoverOUnicoItem() {
        Reserva reserva = new Reserva(50L, 1L, List.of(new ItemReserva(1L, 10L, "Losartana 50mg", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        Medicamento losartana = new Medicamento(10L, "Losartana 50mg", 4);

        when(reservaGateway.buscarPorId(50L)).thenReturn(Optional.of(reserva));
        when(medicamentoGateway.buscarPorId(10L)).thenReturn(Optional.of(losartana));
        when(reservaGateway.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RemoverUltimoItemReservaUseCase.Resultado resultado = useCase.executar(50L);

        assertThat(resultado.itemRemovido().getNomeMedicamento()).isEqualTo("Losartana 50mg");
        assertThat(resultado.reservaAtualizada()).isEmpty();
        assertThat(reserva.getStatus()).isEqualTo(ReservaStatus.CANCELADA);
        assertThat(losartana.getQuantidadeEstoque()).isEqualTo(5);
    }
}
