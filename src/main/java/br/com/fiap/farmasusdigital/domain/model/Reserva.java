package br.com.fiap.farmasusdigital.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEstaAtivaException;
import br.com.fiap.farmasusdigital.domain.service.ReservaExpiracaoPolicy;

/**
 * Agregado raiz da reserva. Concentra as regras do ciclo de vida:
 * so pode ser finalizada por retirada, cancelamento ou expiracao,
 * e uma vez finalizada nao pode mudar de estado novamente.
 */
public class Reserva {

    private Long id;
    private final Long pacienteId;
    private final List<ItemReserva> itens;
    private ReservaStatus status;
    private final LocalDateTime dataCriacao;
    private final LocalDateTime dataExpiracao;
    private LocalDateTime dataFinalizacao;
    private boolean lembreteEnviado;

    public Reserva(Long id, Long pacienteId, List<ItemReserva> itens, ReservaStatus status,
                    LocalDateTime dataCriacao, LocalDateTime dataExpiracao, LocalDateTime dataFinalizacao) {
        this(id, pacienteId, itens, status, dataCriacao, dataExpiracao, dataFinalizacao, false);
    }

    public Reserva(Long id, Long pacienteId, List<ItemReserva> itens, ReservaStatus status,
                    LocalDateTime dataCriacao, LocalDateTime dataExpiracao, LocalDateTime dataFinalizacao,
                    boolean lembreteEnviado) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.itens = new ArrayList<>(itens);
        this.status = status;
        this.dataCriacao = dataCriacao;
        this.dataExpiracao = dataExpiracao;
        this.dataFinalizacao = dataFinalizacao;
        this.lembreteEnviado = lembreteEnviado;
    }

    public static Reserva abrirNova(Long pacienteId, LocalDateTime agora) {
        LocalDateTime dataExpiracao = ReservaExpiracaoPolicy.calcularDataExpiracao(agora);
        return new Reserva(null, pacienteId, new ArrayList<>(), ReservaStatus.ATIVA, agora, dataExpiracao, null);
    }

    public void adicionarItem(ItemReserva item) {
        garantirAtiva();
        itens.add(item);
    }

    /**
     * Remove o ultimo item adicionado (o mais recente), usado quando o
     * paciente se arrepende de um medicamento antes de finalizar a reserva.
     */
    public ItemReserva removerUltimoItem() {
        garantirAtiva();
        if (itens.isEmpty()) {
            throw new IllegalStateException("Reserva nao possui itens para remover");
        }
        return itens.remove(itens.size() - 1);
    }

    public void cancelar(LocalDateTime agora) {
        garantirAtiva();
        this.status = ReservaStatus.CANCELADA;
        this.dataFinalizacao = agora;
    }

    public void retirar(LocalDateTime agora) {
        garantirAtiva();
        this.status = ReservaStatus.RETIRADA;
        this.dataFinalizacao = agora;
    }

    public void expirar(LocalDateTime agora) {
        garantirAtiva();
        this.status = ReservaStatus.EXPIRADA;
        this.dataFinalizacao = agora;
    }

    public boolean estaVencida(LocalDateTime agora) {
        return status == ReservaStatus.ATIVA && agora.isAfter(dataExpiracao);
    }

    /**
     * @return true se ja passou (ou esta dentro de) o horario de aviso e o
     * lembrete de expiracao ainda nao foi enviado para esta reserva.
     */
    public boolean precisaDeLembrete(LocalDateTime limiteAviso) {
        return status == ReservaStatus.ATIVA && !lembreteEnviado && !dataExpiracao.isAfter(limiteAviso);
    }

    public void marcarLembreteEnviado() {
        this.lembreteEnviado = true;
    }

    public boolean estaAtiva() {
        return status == ReservaStatus.ATIVA;
    }

    private void garantirAtiva() {
        if (status != ReservaStatus.ATIVA) {
            throw new ReservaNaoEstaAtivaException(id);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public List<ItemReserva> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public ReservaStatus getStatus() {
        return status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }

    public LocalDateTime getDataFinalizacao() {
        return dataFinalizacao;
    }

    public boolean isLembreteEnviado() {
        return lembreteEnviado;
    }
}
