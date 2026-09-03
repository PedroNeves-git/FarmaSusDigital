package br.com.fiap.farmasusdigital.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.farmasusdigital.application.gateway.MedicamentoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.farmasusdigital.domain.exception.MedicamentoNaoEncontradoException;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.domain.model.Reserva;

@ExtendWith(MockitoExtension.class)
class AdicionarItemReservaUseCaseTest {

    @Mock
    private ReservaGateway reservaGateway;

    @Mock
    private MedicamentoGateway medicamentoGateway;

    private AdicionarItemReservaUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clockFixo = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);
        useCase = new AdicionarItemReservaUseCase(reservaGateway, medicamentoGateway, clockFixo);
    }

    @Test
    void deveCriarNovaReservaQuandoPacienteNaoPossuiReservaAtiva() {
        Medicamento medicamento = new Medicamento(10L, "Dipirona 500mg", 5);
        when(medicamentoGateway.buscarPorId(10L)).thenReturn(Optional.of(medicamento));
        when(reservaGateway.buscarAtivaPorPaciente(1L)).thenReturn(Optional.empty());
        when(reservaGateway.salvar(any())).thenAnswer(this::ecoarComId);

        Reserva reserva = useCase.executar(1L, 10L, 1);

        assertThat(reserva.getItens()).hasSize(1);
        assertThat(reserva.getItens().get(0).getNomeMedicamento()).isEqualTo("Dipirona 500mg");
        assertThat(medicamento.getQuantidadeEstoque()).isEqualTo(4);
    }

    @Test
    void deveReutilizarReservaAtivaExistenteAoAdicionarSegundoItem() {
        Medicamento medicamento = new Medicamento(20L, "Losartana 50mg", 5);
        when(medicamentoGateway.buscarPorId(20L)).thenReturn(Optional.of(medicamento));

        Reserva reservaExistente = Reserva.abrirNova(1L, java.time.LocalDateTime.now());
        reservaExistente.setId(99L);
        when(reservaGateway.buscarAtivaPorPaciente(1L)).thenReturn(Optional.of(reservaExistente));
        when(reservaGateway.salvar(any())).thenAnswer(this::ecoarComId);

        Reserva reserva = useCase.executar(1L, 20L, 1);

        assertThat(reserva.getId()).isEqualTo(99L);
        assertThat(reserva.getItens()).hasSize(1);
    }

    @Test
    void deveLancarExcecaoQuandoMedicamentoNaoEncontrado() {
        when(medicamentoGateway.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(1L, 999L, 1))
                .isInstanceOf(MedicamentoNaoEncontradoException.class);
    }

    @Test
    void deveLancarExcecaoQuandoEstoqueInsuficiente() {
        Medicamento medicamento = new Medicamento(30L, "Metformina 850mg", 0);
        when(medicamentoGateway.buscarPorId(30L)).thenReturn(Optional.of(medicamento));

        assertThatThrownBy(() -> useCase.executar(1L, 30L, 1))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    private Reserva ecoarComId(InvocationOnMock invocation) {
        Reserva reserva = invocation.getArgument(0);
        if (reserva.getId() == null) {
            reserva.setId(100L);
        }
        return reserva;
    }
}
