package br.com.fiap.farmasusdigital.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.fiap.farmasusdigital.domain.exception.EstoqueInsuficienteException;

class MedicamentoTest {

    @Test
    void deveDecrementarEstoqueAoReservar() {
        Medicamento medicamento = new Medicamento(1L, "Losartana", 10);

        medicamento.reservar(3);

        assertThat(medicamento.getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    void naoDevePermitirReservarMaisDoQueOEstoqueDisponivel() {
        Medicamento medicamento = new Medicamento(1L, "Losartana", 2);

        assertThatThrownBy(() -> medicamento.reservar(3))
                .isInstanceOf(EstoqueInsuficienteException.class);
        assertThat(medicamento.getQuantidadeEstoque()).isEqualTo(2);
    }

    @Test
    void deveDevolverQuantidadeAoEstoque() {
        Medicamento medicamento = new Medicamento(1L, "Losartana", 5);
        medicamento.reservar(2);

        medicamento.devolverAoEstoque(2);

        assertThat(medicamento.getQuantidadeEstoque()).isEqualTo(5);
    }
}
