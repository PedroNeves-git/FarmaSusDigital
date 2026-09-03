package br.com.fiap.farmasusdigital.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEstaAtivaException;

class ReservaTest {

    @Test
    void deveAbrirNovaReservaAtivaComDataDeExpiracaoCalculada() {
        LocalDateTime agora = LocalDateTime.of(2026, 9, 1, 10, 0);

        Reserva reserva = Reserva.abrirNova(1L, agora);

        assertThat(reserva.estaAtiva()).isTrue();
        assertThat(reserva.getStatus()).isEqualTo(ReservaStatus.ATIVA);
        assertThat(reserva.getDataExpiracao()).isAfter(reserva.getDataCriacao());
        assertThat(reserva.getItens()).isEmpty();
    }

    @Test
    void deveAdicionarItensAUmaReservaAtiva() {
        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());

        reserva.adicionarItem(new ItemReserva(null, 10L, "Dipirona", 1));

        assertThat(reserva.getItens()).hasSize(1);
        assertThat(reserva.getItens().get(0).getNomeMedicamento()).isEqualTo("Dipirona");
    }

    @Test
    void deveCancelarReservaAtiva() {
        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());

        reserva.cancelar(LocalDateTime.now());

        assertThat(reserva.getStatus()).isEqualTo(ReservaStatus.CANCELADA);
        assertThat(reserva.getDataFinalizacao()).isNotNull();
    }

    @Test
    void naoDevePermitirCancelarReservaJaFinalizada() {
        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.retirar(LocalDateTime.now());

        assertThatThrownBy(() -> reserva.cancelar(LocalDateTime.now()))
                .isInstanceOf(ReservaNaoEstaAtivaException.class);
    }

    @Test
    void deveConsiderarVencidaQuandoAgoraForApósDataDeExpiracao() {
        LocalDateTime criacao = LocalDateTime.of(2026, 9, 1, 10, 0);
        Reserva reserva = Reserva.abrirNova(1L, criacao);

        boolean vencidaAntes = reserva.estaVencida(criacao.plusHours(1));
        boolean vencidaDepois = reserva.estaVencida(reserva.getDataExpiracao().plusMinutes(1));

        assertThat(vencidaAntes).isFalse();
        assertThat(vencidaDepois).isTrue();
    }

    @Test
    void naoDevePermitirAdicionarItemEmReservaCancelada() {
        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.cancelar(LocalDateTime.now());

        assertThatThrownBy(() -> reserva.adicionarItem(new ItemReserva(null, 1L, "Dipirona", 1)))
                .isInstanceOf(ReservaNaoEstaAtivaException.class);
    }

    @Test
    void deveRemoverOUltimoItemAdicionado() {
        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.adicionarItem(new ItemReserva(null, 1L, "Dipirona", 1));
        reserva.adicionarItem(new ItemReserva(null, 2L, "Losartana", 1));

        ItemReserva removido = reserva.removerUltimoItem();

        assertThat(removido.getNomeMedicamento()).isEqualTo("Losartana");
        assertThat(reserva.getItens()).hasSize(1);
        assertThat(reserva.getItens().get(0).getNomeMedicamento()).isEqualTo("Dipirona");
    }

    @Test
    void naoDevePermitirRemoverItemDeReservaSemItens() {
        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());

        assertThatThrownBy(reserva::removerUltimoItem).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void devePrecisarDeLembreteQuandoDataDeExpiracaoEstaDentroDoLimiteDeAviso() {
        LocalDateTime criacao = LocalDateTime.of(2026, 9, 1, 10, 0);
        Reserva reserva = Reserva.abrirNova(1L, criacao);

        boolean antesDoLimite = reserva.precisaDeLembrete(reserva.getDataExpiracao().minusHours(2));
        boolean dentroDoLimite = reserva.precisaDeLembrete(reserva.getDataExpiracao().plusMinutes(1));

        assertThat(antesDoLimite).isFalse();
        assertThat(dentroDoLimite).isTrue();
    }

    @Test
    void naoDevePrecisarDeLembreteDepoisDeMarcadoComoEnviado() {
        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.marcarLembreteEnviado();

        assertThat(reserva.precisaDeLembrete(reserva.getDataExpiracao().plusMinutes(1))).isFalse();
        assertThat(reserva.isLembreteEnviado()).isTrue();
    }
}
