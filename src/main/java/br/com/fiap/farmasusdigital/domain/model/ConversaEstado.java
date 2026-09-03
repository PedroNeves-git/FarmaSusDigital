package br.com.fiap.farmasusdigital.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa o "estado" atual de uma conversa do bot com um paciente,
 * necessario porque o Telegram envia uma mensagem por vez e o fluxo
 * (cadastro, escolha de medicamentos, confirmacao) acontece em varias etapas.
 */
public class ConversaEstado {

    private final String telefone;
    private EstadoConversa estado;
    private Long reservaRascunhoId;
    private List<Long> candidatosMedicamentoIds;
    private String cpfTemporario;
    private int quantidadeDesejada = 1;
    private LocalDateTime dataUltimaAtualizacao;

    public ConversaEstado(String telefone, EstadoConversa estado, Long reservaRascunhoId) {
        this(telefone, estado, reservaRascunhoId, List.of());
    }

    public ConversaEstado(String telefone, EstadoConversa estado, Long reservaRascunhoId,
                           List<Long> candidatosMedicamentoIds) {
        this.telefone = telefone;
        this.estado = estado;
        this.reservaRascunhoId = reservaRascunhoId;
        this.candidatosMedicamentoIds = candidatosMedicamentoIds == null
                ? new ArrayList<>()
                : new ArrayList<>(candidatosMedicamentoIds);
    }

    public String getTelefone() {
        return telefone;
    }

    public EstadoConversa getEstado() {
        return estado;
    }

    public void setEstado(EstadoConversa estado) {
        this.estado = estado;
    }

    public Long getReservaRascunhoId() {
        return reservaRascunhoId;
    }

    public void setReservaRascunhoId(Long reservaRascunhoId) {
        this.reservaRascunhoId = reservaRascunhoId;
    }

    public List<Long> getCandidatosMedicamentoIds() {
        return Collections.unmodifiableList(candidatosMedicamentoIds);
    }

    public void setCandidatosMedicamentoIds(List<Long> candidatosMedicamentoIds) {
        this.candidatosMedicamentoIds = candidatosMedicamentoIds == null
                ? new ArrayList<>()
                : new ArrayList<>(candidatosMedicamentoIds);
    }

    public String getCpfTemporario() {
        return cpfTemporario;
    }

    public void setCpfTemporario(String cpfTemporario) {
        this.cpfTemporario = cpfTemporario;
    }

    public int getQuantidadeDesejada() {
        return quantidadeDesejada;
    }

    public void setQuantidadeDesejada(int quantidadeDesejada) {
        this.quantidadeDesejada = quantidadeDesejada;
    }

    public LocalDateTime getDataUltimaAtualizacao() {
        return dataUltimaAtualizacao;
    }

    public void setDataUltimaAtualizacao(LocalDateTime dataUltimaAtualizacao) {
        this.dataUltimaAtualizacao = dataUltimaAtualizacao;
    }
}
