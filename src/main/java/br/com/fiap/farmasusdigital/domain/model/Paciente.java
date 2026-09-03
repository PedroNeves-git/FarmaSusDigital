package br.com.fiap.farmasusdigital.domain.model;

public class Paciente {

    private Long id;
    private final String cpf;
    private final String nomeCompleto;
    private final String telefone;

    public Paciente(Long id, String cpf, String nomeCompleto, String telefone) {
        this.id = id;
        this.cpf = cpf;
        this.nomeCompleto = nomeCompleto;
        this.telefone = telefone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCpf() {
        return cpf;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getTelefone() {
        return telefone;
    }
}
