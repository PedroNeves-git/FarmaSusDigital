package br.com.fiap.farmasusdigital.application.usecase;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import br.com.fiap.farmasusdigital.application.gateway.ConversaEstadoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.exception.CpfInvalidoException;
import br.com.fiap.farmasusdigital.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.farmasusdigital.domain.exception.MedicamentoNaoEncontradoException;
import br.com.fiap.farmasusdigital.domain.model.ConversaEstado;
import br.com.fiap.farmasusdigital.domain.model.EstadoConversa;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.domain.model.Paciente;
import br.com.fiap.farmasusdigital.domain.model.Reserva;
import br.com.fiap.farmasusdigital.domain.service.CpfValidator;
import br.com.fiap.farmasusdigital.domain.service.IntencaoMatcher;

/**
 * Orquestra a "maquina de estados" da conversa do bot do Telegram,
 * lidando com os 3 cenarios de atendimento descritos no fluxo do produto:
 * novo usuario, usuario sem reserva ativa e usuario com reserva ativa.
 */
@Component
public class ProcessarMensagemTelegramUseCase {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConversaEstadoGateway conversaEstadoGateway;
    private final ReservaGateway reservaGateway;
    private final BuscarPacienteUseCase buscarPacienteUseCase;
    private final CadastrarPacienteUseCase cadastrarPacienteUseCase;
    private final BuscarMedicamentosUseCase buscarMedicamentosUseCase;
    private final AdicionarItemReservaUseCase adicionarItemReservaUseCase;
    private final RemoverUltimoItemReservaUseCase removerUltimoItemReservaUseCase;
    private final CancelarReservaUseCase cancelarReservaUseCase;

    public ProcessarMensagemTelegramUseCase(ConversaEstadoGateway conversaEstadoGateway,
                                             ReservaGateway reservaGateway,
                                             BuscarPacienteUseCase buscarPacienteUseCase,
                                             CadastrarPacienteUseCase cadastrarPacienteUseCase,
                                             BuscarMedicamentosUseCase buscarMedicamentosUseCase,
                                             AdicionarItemReservaUseCase adicionarItemReservaUseCase,
                                             RemoverUltimoItemReservaUseCase removerUltimoItemReservaUseCase,
                                             CancelarReservaUseCase cancelarReservaUseCase) {
        this.conversaEstadoGateway = conversaEstadoGateway;
        this.reservaGateway = reservaGateway;
        this.buscarPacienteUseCase = buscarPacienteUseCase;
        this.cadastrarPacienteUseCase = cadastrarPacienteUseCase;
        this.buscarMedicamentosUseCase = buscarMedicamentosUseCase;
        this.adicionarItemReservaUseCase = adicionarItemReservaUseCase;
        this.removerUltimoItemReservaUseCase = removerUltimoItemReservaUseCase;
        this.cancelarReservaUseCase = cancelarReservaUseCase;
    }

    public String executar(String telefone, String texto) {
        Optional<ConversaEstado> conversaEstado = conversaEstadoGateway.buscarPorTelefone(telefone);
        if (conversaEstado.isPresent()) {
            return continuarConversa(telefone, texto, conversaEstado.get());
        }
        return iniciarConversa(telefone);
    }

    private String iniciarConversa(String telefone) {
        Optional<Paciente> paciente = buscarPacienteUseCase.executarPorTelefone(telefone);
        if (paciente.isEmpty()) {
            conversaEstadoGateway.salvar(new ConversaEstado(telefone, EstadoConversa.AGUARDANDO_CPF, null));
            return "Seja bem-vindo ao serviço de reservas de medicamentos do SUS! "
                    + "Para iniciarmos seu atendimento, por favor, informe seu CPF:";
        }

        Optional<Reserva> reservaAtiva = reservaGateway.buscarAtivaPorPaciente(paciente.get().getId());
        if (reservaAtiva.isEmpty()) {
            conversaEstadoGateway.salvar(new ConversaEstado(telefone, EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO, null));
            return "Seja bem-vindo, " + paciente.get().getNomeCompleto()
                    + "! Informe o nome de qual medicamento deseja reservar:";
        }

        ConversaEstado novoEstado = new ConversaEstado(telefone, EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO,
                reservaAtiva.get().getId());
        conversaEstadoGateway.salvar(novoEstado);
        return "Seja bem-vindo, " + paciente.get().getNomeCompleto() + "! Você possui uma reserva ativa dos itens: "
                + listarNomesItens(reservaAtiva.get()) + ". Deseja cancelar esta reserva?";
    }

    private String continuarConversa(String telefone, String texto, ConversaEstado conversaEstado) {
        EstadoConversa estadoAtual = conversaEstado.getEstado();

        if (IntencaoMatcher.pareceAjuda(texto)) {
            return mensagemAjudaPara(estadoAtual);
        }

        if (isConstruindoReserva(estadoAtual)) {
            if (conversaEstado.getReservaRascunhoId() != null && IntencaoMatcher.pareceConsultaDeItens(texto)) {
                return tratarConsultaDeItens(conversaEstado);
            }
            if (IntencaoMatcher.pareceCancelamentoTotal(texto)) {
                return tratarCancelamentoDuranteConstrucao(telefone, conversaEstado);
            }
        }

        return switch (estadoAtual) {
            case AGUARDANDO_CPF -> tratarCpf(texto, conversaEstado);
            case AGUARDANDO_NOME_PACIENTE -> tratarNomePaciente(telefone, texto, conversaEstado);
            case AGUARDANDO_NOME_MEDICAMENTO -> tratarNovoItem(telefone, texto, conversaEstado);
            case AGUARDANDO_ESCOLHA_MEDICAMENTO -> tratarEscolhaMedicamento(telefone, texto, conversaEstado);
            case AGUARDANDO_MAIS_ITENS_OU_FINALIZAR -> tratarMaisItensOuFinalizar(telefone, texto, conversaEstado);
            case AGUARDANDO_CONFIRMACAO_CANCELAMENTO -> tratarConfirmacaoCancelamento(telefone, texto, conversaEstado);
        };
    }

    private boolean isConstruindoReserva(EstadoConversa estado) {
        return estado == EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO
                || estado == EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO
                || estado == EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR;
    }

    private String mensagemAjudaPara(EstadoConversa estado) {
        return switch (estado) {
            case AGUARDANDO_CPF -> "Informe seu CPF (com ou sem pontuação) para começarmos seu cadastro.";
            case AGUARDANDO_NOME_PACIENTE -> "Informe seu nome completo (nome e sobrenome) para concluir o "
                    + "cadastro. Se digitou o CPF errado, diga \"corrigir CPF\" para informá-lo novamente.";
            case AGUARDANDO_NOME_MEDICAMENTO -> "Informe o nome do medicamento que deseja reservar (pode ser só "
                    + "uma parte do nome, ex.: \"dipirona\"). Também dá para informar a quantidade antes do "
                    + "nome, ex.: \"2 dipirona\". Se quiser desistir, digite \"cancelar\".";
            case AGUARDANDO_ESCOLHA_MEDICAMENTO -> "Encontramos mais de uma opção parecida - responda com o "
                    + "número da opção desejada, ou digite \"cancelar\" para desistir.";
            case AGUARDANDO_MAIS_ITENS_OU_FINALIZAR -> "Você pode: informar o nome de outro medicamento para "
                    + "adicionar à reserva; digitar \"finalizar\" para concluir; digitar \"remover esse\" para "
                    + "tirar o último item adicionado; ou perguntar \"o que eu já reservei?\" para ver a lista "
                    + "atual.";
            case AGUARDANDO_CONFIRMACAO_CANCELAMENTO -> "Responda \"sim\" para confirmar o cancelamento da "
                    + "reserva, ou \"não\" para mantê-la ativa.";
        };
    }

    private String tratarConsultaDeItens(ConversaEstado conversaEstado) {
        Reserva reserva = reservaGateway.buscarPorId(conversaEstado.getReservaRascunhoId()).orElseThrow();
        return "Até agora você tem reservado: " + listarNomesItens(reserva)
                + ". Deseja reservar mais algum medicamento ou deseja finalizar a reserva?";
    }

    private String tratarCancelamentoDuranteConstrucao(String telefone, ConversaEstado conversaEstado) {
        Long reservaRascunhoId = conversaEstado.getReservaRascunhoId();
        if (reservaRascunhoId == null) {
            conversaEstadoGateway.remover(telefone);
            return "Tudo bem, encerrei o atendimento sem reservar nada. Quando quiser, é só mandar uma mensagem "
                    + "para começar de novo.";
        }

        Reserva reserva = reservaGateway.buscarPorId(reservaRascunhoId).orElseThrow();
        conversaEstado.setEstado(EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO);
        conversaEstado.setCandidatosMedicamentoIds(List.of());
        conversaEstadoGateway.salvar(conversaEstado);
        return "Você tem certeza que deseja cancelar a reserva com os itens: " + listarNomesItens(reserva)
                + "? Responda sim ou não.";
    }

    private String tratarCpf(String texto, ConversaEstado conversaEstado) {
        String cpfDigitado = texto.trim();
        if (!CpfValidator.isValido(cpfDigitado)) {
            return "Esse CPF não parece válido. Por favor, informe novamente, ex.: 123.456.789-00:";
        }

        conversaEstado.setCpfTemporario(CpfValidator.somenteDigitos(cpfDigitado));
        conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_PACIENTE);
        conversaEstadoGateway.salvar(conversaEstado);
        return "CPF válido! Agora, informe seu nome completo:";
    }

    private String tratarNomePaciente(String telefone, String texto, ConversaEstado conversaEstado) {
        if (IntencaoMatcher.pareceCorrecaoDeCadastro(texto)) {
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_CPF);
            conversaEstado.setCpfTemporario(null);
            conversaEstadoGateway.salvar(conversaEstado);
            return "Sem problemas! Informe seu CPF novamente:";
        }

        String nome = texto.trim();
        if (nome.isBlank() || !nome.contains(" ")) {
            return "Por favor, informe seu nome completo (nome e sobrenome):";
        }

        try {
            Paciente paciente = cadastrarPacienteUseCase.executar(conversaEstado.getCpfTemporario(), nome, telefone);
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
            conversaEstado.setCpfTemporario(null);
            conversaEstadoGateway.salvar(conversaEstado);
            return "Cadastro realizado com sucesso, " + paciente.getNomeCompleto()
                    + "! Informe o nome de qual medicamento deseja reservar:";
        } catch (CpfInvalidoException e) {
            conversaEstadoGateway.remover(telefone);
            return "Houve um problema com o CPF informado. Por favor, comece novamente informando seu CPF:";
        }
    }

    private String tratarNovoItem(String telefone, String texto, ConversaEstado conversaEstado) {
        Optional<Paciente> paciente = buscarPacienteUseCase.executarPorTelefone(telefone);
        if (paciente.isEmpty()) {
            conversaEstadoGateway.remover(telefone);
            return iniciarConversa(telefone);
        }

        IntencaoMatcher.TextoComQuantidade interpretado = IntencaoMatcher.extrairQuantidade(texto.trim());
        List<Medicamento> encontrados = buscarMedicamentosUseCase.executarPorNomeSemelhante(interpretado.nomeRestante());

        if (encontrados.isEmpty()) {
            return "Não encontramos nenhum medicamento parecido com \"" + interpretado.nomeRestante()
                    + "\" no estoque. Por favor, informe o nome de outro medicamento:";
        }

        if (encontrados.size() > 1) {
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO);
            conversaEstado.setCandidatosMedicamentoIds(encontrados.stream().map(Medicamento::getId).toList());
            conversaEstado.setQuantidadeDesejada(interpretado.quantidade());
            conversaEstadoGateway.salvar(conversaEstado);
            return "Encontramos mais de um medicamento parecido com \"" + interpretado.nomeRestante()
                    + "\". Responda com o número da opção desejada:\n" + listarOpcoes(encontrados);
        }

        return adicionarItemEResponder(telefone, conversaEstado, paciente.get().getId(), encontrados.get(0),
                interpretado.quantidade());
    }

    private String tratarEscolhaMedicamento(String telefone, String texto, ConversaEstado conversaEstado) {
        List<Long> candidatos = conversaEstado.getCandidatosMedicamentoIds();
        Integer indiceEscolhido = tentarConverterParaIndice(texto, candidatos.size());
        if (indiceEscolhido == null) {
            return "Não entendi sua escolha. Responda apenas com o número da opção (1 a " + candidatos.size() + ").";
        }

        Optional<Paciente> paciente = buscarPacienteUseCase.executarPorTelefone(telefone);
        if (paciente.isEmpty()) {
            conversaEstadoGateway.remover(telefone);
            return iniciarConversa(telefone);
        }

        Long medicamentoEscolhidoId = candidatos.get(indiceEscolhido - 1);
        Medicamento medicamento = buscarMedicamentosUseCase.executarPorId(medicamentoEscolhidoId)
                .orElseThrow(() -> new MedicamentoNaoEncontradoException(String.valueOf(medicamentoEscolhidoId)));

        return adicionarItemEResponder(telefone, conversaEstado, paciente.get().getId(), medicamento,
                conversaEstado.getQuantidadeDesejada());
    }

    private String adicionarItemEResponder(String telefone, ConversaEstado conversaEstado, Long pacienteId,
                                            Medicamento medicamento, int quantidade) {
        try {
            Reserva reserva = adicionarItemReservaUseCase.executar(pacienteId, medicamento.getId(), quantidade);
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR);
            conversaEstado.setReservaRascunhoId(reserva.getId());
            conversaEstado.setCandidatosMedicamentoIds(List.of());
            conversaEstado.setQuantidadeDesejada(1);
            conversaEstadoGateway.salvar(conversaEstado);
            String prefixoQuantidade = quantidade > 1 ? quantidade + "x " : "";
            return "Medicamento " + prefixoQuantidade + "\"" + medicamento.getNome() + "\" adicionado à sua "
                    + "reserva. Deseja reservar mais algum medicamento ou deseja finalizar a reserva?";
        } catch (EstoqueInsuficienteException e) {
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
            conversaEstado.setCandidatosMedicamentoIds(List.of());
            conversaEstado.setQuantidadeDesejada(1);
            conversaEstadoGateway.salvar(conversaEstado);
            return "No momento não há estoque disponível para \"" + medicamento.getNome()
                    + "\" na quantidade solicitada. Por favor, informe o nome de outro medicamento:";
        }
    }

    private String tratarMaisItensOuFinalizar(String telefone, String texto, ConversaEstado conversaEstado) {
        if (IntencaoMatcher.pareceRemocaoDeItem(texto)) {
            return tratarRemocaoUltimoItem(telefone, conversaEstado);
        }

        if (IntencaoMatcher.pareceFinalizacao(texto)) {
            Reserva reserva = reservaGateway.buscarPorId(conversaEstado.getReservaRascunhoId())
                    .orElseThrow();
            conversaEstadoGateway.remover(telefone);
            return "Reserva finalizada com sucesso! Itens reservados: " + listarNomesItens(reserva)
                    + ". Retire seus medicamentos até " + reserva.getDataExpiracao().format(FORMATO_DATA)
                    + ", após esse prazo a reserva será cancelada automaticamente.";
        }

        return tratarNovoItem(telefone, texto, conversaEstado);
    }

    private String tratarRemocaoUltimoItem(String telefone, ConversaEstado conversaEstado) {
        RemoverUltimoItemReservaUseCase.Resultado resultado =
                removerUltimoItemReservaUseCase.executar(conversaEstado.getReservaRascunhoId());

        if (resultado.reservaAtualizada().isEmpty()) {
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
            conversaEstado.setReservaRascunhoId(null);
            conversaEstadoGateway.salvar(conversaEstado);
            return "Removi \"" + resultado.itemRemovido().getNomeMedicamento()
                    + "\" da sua reserva. Como era o único item, cancelei a reserva. "
                    + "Se quiser começar outra, informe o nome de um medicamento:";
        }

        return "Removi \"" + resultado.itemRemovido().getNomeMedicamento() + "\" da sua reserva. Itens restantes: "
                + listarNomesItens(resultado.reservaAtualizada().get())
                + ". Deseja reservar mais algum medicamento ou deseja finalizar a reserva?";
    }

    private String tratarConfirmacaoCancelamento(String telefone, String texto, ConversaEstado conversaEstado) {
        Boolean confirmou = IntencaoMatcher.interpretarConfirmacao(texto);
        if (confirmou == null) {
            return "Não entendi. Você quer mesmo cancelar a reserva? Responda sim ou não.";
        }

        conversaEstadoGateway.remover(telefone);
        if (confirmou) {
            cancelarReservaUseCase.executar(conversaEstado.getReservaRascunhoId());
            return "Sua reserva foi cancelada com sucesso e os medicamentos foram liberados de volta ao estoque.";
        }
        return "Ok, sua reserva ativa foi mantida.";
    }

    private String listarNomesItens(Reserva reserva) {
        return reserva.getItens().stream()
                .map(item -> item.getQuantidade() > 1
                        ? item.getNomeMedicamento() + " (x" + item.getQuantidade() + ")"
                        : item.getNomeMedicamento())
                .collect(Collectors.joining(", "));
    }

    private String listarOpcoes(List<Medicamento> medicamentos) {
        return IntStream.range(0, medicamentos.size())
                .mapToObj(indice -> (indice + 1) + ". " + medicamentos.get(indice).getNome())
                .collect(Collectors.joining("\n"));
    }

    private Integer tentarConverterParaIndice(String texto, int tamanho) {
        try {
            int valor = Integer.parseInt(texto.trim());
            return valor >= 1 && valor <= tamanho ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
