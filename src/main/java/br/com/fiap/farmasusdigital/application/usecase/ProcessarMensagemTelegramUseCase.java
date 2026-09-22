package br.com.fiap.farmasusdigital.application.usecase;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import br.com.fiap.farmasusdigital.domain.exception.ReservaNaoEncontradaException;
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
    private static final String OPCAO_CANCELAR = "Cancelar";
    private static final List<String> OPCOES_SIM_NAO = List.of("Sim", "Não");
    private static final List<String> OPCOES_FINALIZAR_CANCELAR = List.of("Finalizar", OPCAO_CANCELAR);

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

    public RespostaConversa executar(String telefone, String texto) {
        Optional<ConversaEstado> conversaEstado = conversaEstadoGateway.buscarPorTelefone(telefone);
        if (conversaEstado.isPresent()) {
            return continuarConversa(telefone, texto, conversaEstado.get());
        }
        return iniciarConversa(telefone);
    }

    private RespostaConversa iniciarConversa(String telefone) {
        Optional<Paciente> paciente = buscarPacienteUseCase.executarPorTelefone(telefone);
        if (paciente.isEmpty()) {
            conversaEstadoGateway.salvar(new ConversaEstado(telefone, EstadoConversa.AGUARDANDO_CPF, null));
            return RespostaConversa.semOpcoes("Seja bem-vindo ao serviço de reservas de medicamentos do SUS! "
                    + "Para iniciarmos seu atendimento, por favor, informe seu CPF:");
        }

        Optional<Reserva> reservaAtiva = reservaGateway.buscarAtivaPorPaciente(paciente.get().getId());
        if (reservaAtiva.isEmpty()) {
            conversaEstadoGateway.salvar(new ConversaEstado(telefone, EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO, null));
            return RespostaConversa.semOpcoes("Seja bem-vindo, <b>" + escaparHtml(paciente.get().getNomeCompleto())
                    + "</b>! Informe o nome de qual medicamento deseja reservar:");
        }

        Reserva reserva = reservaAtiva.get();
        ConversaEstado novoEstado = new ConversaEstado(telefone, EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO,
                reserva.getId());
        conversaEstadoGateway.salvar(novoEstado);
        return RespostaConversa.comOpcoes(
                "Seja bem-vindo, <b>" + escaparHtml(paciente.get().getNomeCompleto())
                        + "</b>! Você possui uma reserva ativa dos itens:\n" + listarItensEmBullets(reserva)
                        + "\n\nEla expira em " + reserva.getDataExpiracao().format(FORMATO_DATA)
                        + ", após esse prazo será cancelada automaticamente. Deseja cancelar esta reserva?",
                OPCOES_SIM_NAO);
    }

    private RespostaConversa continuarConversa(String telefone, String texto, ConversaEstado conversaEstado) {
        EstadoConversa estadoAtual = conversaEstado.getEstado();

        if (IntencaoMatcher.pareceAjuda(texto)) {
            return RespostaConversa.comOpcoes(mensagemAjudaPara(estadoAtual), opcoesParaEstado(conversaEstado));
        }

        // cancelar precisa funcionar em qualquer etapa - inclusive no meio do
        // cadastro - para a conversa nunca virar um beco sem saida
        if (estadoAtual != EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO
                && IntencaoMatcher.pareceCancelamentoTotal(texto)) {
            return tratarCancelamentoGlobal(telefone, conversaEstado);
        }

        if (isConstruindoReserva(estadoAtual) && conversaEstado.getReservaRascunhoId() != null
                && IntencaoMatcher.pareceConsultaDeItens(texto)) {
            return tratarConsultaDeItens(conversaEstado);
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

    /**
     * Opcoes de resposta rapida (botoes) coerentes com o que se espera do
     * paciente em cada estado. Estados de texto livre (CPF, nome, nome de
     * medicamento) nao tem botao - so faz sentido digitar.
     */
    private List<String> opcoesParaEstado(ConversaEstado conversaEstado) {
        return switch (conversaEstado.getEstado()) {
            case AGUARDANDO_CPF, AGUARDANDO_NOME_PACIENTE, AGUARDANDO_NOME_MEDICAMENTO -> List.of();
            case AGUARDANDO_ESCOLHA_MEDICAMENTO ->
                    opcoesEscolhaComCancelar(conversaEstado.getCandidatosMedicamentoIds().size());
            case AGUARDANDO_MAIS_ITENS_OU_FINALIZAR -> OPCOES_FINALIZAR_CANCELAR;
            case AGUARDANDO_CONFIRMACAO_CANCELAMENTO -> OPCOES_SIM_NAO;
        };
    }

    private List<String> opcoesEscolhaComCancelar(int quantidade) {
        List<String> opcoes = new ArrayList<>(
                IntStream.rangeClosed(1, quantidade).mapToObj(String::valueOf).toList());
        opcoes.add(OPCAO_CANCELAR);
        return opcoes;
    }

    private String mensagemAjudaPara(EstadoConversa estado) {
        return switch (estado) {
            case AGUARDANDO_CPF -> "Informe seu CPF (com ou sem pontuação) para começarmos seu cadastro. "
                    + "Se quiser desistir, digite \"cancelar\".";
            case AGUARDANDO_NOME_PACIENTE -> "Informe seu nome completo (nome e sobrenome) para concluir o "
                    + "cadastro. Se digitou o CPF errado, diga \"corrigir CPF\" para informá-lo novamente, "
                    + "ou \"cancelar\" para desistir.";
            case AGUARDANDO_NOME_MEDICAMENTO -> "Informe o nome do medicamento que deseja reservar (pode ser só "
                    + "uma parte do nome, ex.: \"dipirona\"). Também dá para informar a quantidade antes do "
                    + "nome, ex.: \"2 dipirona\". Se quiser desistir, digite \"cancelar\".";
            case AGUARDANDO_ESCOLHA_MEDICAMENTO -> "Toque no número da opção desejada, ou em \"Cancelar\" "
                    + "para desistir.";
            case AGUARDANDO_MAIS_ITENS_OU_FINALIZAR -> "Para adicionar outro medicamento, digite o nome dele. "
                    + "Para tirar o último item adicionado, digite \"remover esse\". Para ver o que já está "
                    + "reservado, pergunte \"o que eu já reservei?\". Ou toque em \"Finalizar\"/\"Cancelar\" "
                    + "abaixo.";
            case AGUARDANDO_CONFIRMACAO_CANCELAMENTO -> "Toque em \"Sim\" para confirmar o cancelamento da "
                    + "reserva, ou em \"Não\" para mantê-la ativa.";
        };
    }

    private RespostaConversa tratarConsultaDeItens(ConversaEstado conversaEstado) {
        Long reservaRascunhoId = conversaEstado.getReservaRascunhoId();
        Reserva reserva = reservaGateway.buscarPorId(reservaRascunhoId)
                .orElseThrow(() -> new ReservaNaoEncontradaException(reservaRascunhoId));
        return RespostaConversa.comOpcoes(
                "Até agora você tem reservado:\n" + listarItensEmBullets(reserva)
                        + "\n\nDeseja reservar mais algum medicamento ou deseja finalizar a reserva?",
                opcoesParaEstado(conversaEstado));
    }

    private RespostaConversa tratarCancelamentoGlobal(String telefone, ConversaEstado conversaEstado) {
        EstadoConversa estadoAtual = conversaEstado.getEstado();

        if (estadoAtual == EstadoConversa.AGUARDANDO_CPF || estadoAtual == EstadoConversa.AGUARDANDO_NOME_PACIENTE) {
            conversaEstadoGateway.remover(telefone);
            return RespostaConversa.semOpcoes(
                    "Tudo bem, cadastro cancelado. Quando quiser começar, é só mandar uma mensagem.");
        }

        Long reservaRascunhoId = conversaEstado.getReservaRascunhoId();
        if (reservaRascunhoId == null) {
            conversaEstadoGateway.remover(telefone);
            return RespostaConversa.semOpcoes("Tudo bem, encerrei o atendimento sem reservar nada. "
                    + "Quando quiser, é só mandar uma mensagem para começar de novo.");
        }

        Reserva reserva = reservaGateway.buscarPorId(reservaRascunhoId)
                .orElseThrow(() -> new ReservaNaoEncontradaException(reservaRascunhoId));
        conversaEstado.setEstado(EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO);
        conversaEstado.setCandidatosMedicamentoIds(List.of());
        conversaEstadoGateway.salvar(conversaEstado);
        return RespostaConversa.comOpcoes(
                "Você tem certeza que deseja cancelar a reserva com os itens:\n" + listarItensEmBullets(reserva)
                        + "\n\nResponda sim ou não.",
                OPCOES_SIM_NAO);
    }

    private RespostaConversa tratarCpf(String texto, ConversaEstado conversaEstado) {
        String cpfDigitado = texto.trim();
        if (!CpfValidator.isValido(cpfDigitado)) {
            return RespostaConversa.semOpcoes("Esse CPF não parece válido. Informe novamente, "
                    + "ex.: 123.456.789-00, ou digite \"cancelar\" para desistir:");
        }

        conversaEstado.setCpfTemporario(CpfValidator.somenteDigitos(cpfDigitado));
        conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_PACIENTE);
        conversaEstadoGateway.salvar(conversaEstado);
        return RespostaConversa.semOpcoes("CPF válido! Agora, informe seu nome completo:");
    }

    private RespostaConversa tratarNomePaciente(String telefone, String texto, ConversaEstado conversaEstado) {
        if (IntencaoMatcher.pareceCorrecaoDeCadastro(texto)) {
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_CPF);
            conversaEstado.setCpfTemporario(null);
            conversaEstadoGateway.salvar(conversaEstado);
            return RespostaConversa.semOpcoes("Sem problemas! Informe seu CPF novamente:");
        }

        String nome = texto.trim();
        if (nome.isBlank() || !nome.contains(" ")) {
            return RespostaConversa.semOpcoes(
                    "Por favor, informe seu nome completo (nome e sobrenome), ou \"cancelar\" para desistir:");
        }

        try {
            Paciente paciente = cadastrarPacienteUseCase.executar(conversaEstado.getCpfTemporario(), nome, telefone);
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
            conversaEstado.setCpfTemporario(null);
            conversaEstadoGateway.salvar(conversaEstado);
            return RespostaConversa.semOpcoes("Cadastro realizado com sucesso, <b>"
                    + escaparHtml(paciente.getNomeCompleto()) + "</b>! Informe o nome de qual medicamento deseja "
                    + "reservar:");
        } catch (CpfInvalidoException e) {
            conversaEstadoGateway.remover(telefone);
            return RespostaConversa.semOpcoes(
                    "Houve um problema com o CPF informado. Por favor, comece novamente informando seu CPF:");
        }
    }

    /**
     * Busca de forma tolerante (parte do nome, sem acento, plural) e sempre
     * apresenta as opcoes encontradas em uma lista numerada para o paciente
     * confirmar - mesmo quando ha so uma opcao em estoque.
     */
    private RespostaConversa tratarNovoItem(String telefone, String texto, ConversaEstado conversaEstado) {
        Optional<Paciente> paciente = buscarPacienteUseCase.executarPorTelefone(telefone);
        if (paciente.isEmpty()) {
            conversaEstadoGateway.remover(telefone);
            return iniciarConversa(telefone);
        }

        IntencaoMatcher.TextoComQuantidade interpretado = IntencaoMatcher.extrairQuantidade(texto.trim());
        if (interpretado.nomeRestante().isBlank()) {
            return RespostaConversa.semOpcoes("Não entendi. Digite o nome do medicamento que deseja reservar "
                    + "(ex.: \"dipirona\"), ou \"cancelar\" para desistir:");
        }

        List<Medicamento> encontrados = buscarMedicamentosUseCase.executarPorNomeSemelhante(interpretado.nomeRestante());

        if (encontrados.isEmpty()) {
            return RespostaConversa.semOpcoes("Não encontramos nenhum medicamento parecido com \""
                    + escaparHtml(interpretado.nomeRestante()) + "\" no estoque. Informe o nome de outro "
                    + "medicamento, ou \"cancelar\" para desistir:");
        }

        conversaEstado.setEstado(EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO);
        conversaEstado.setCandidatosMedicamentoIds(encontrados.stream().map(Medicamento::getId).toList());
        conversaEstado.setQuantidadeDesejada(interpretado.quantidade());
        conversaEstadoGateway.salvar(conversaEstado);

        String opcaoOuOpcoes = encontrados.size() > 1 ? "as seguintes opções" : "a seguinte opção";
        return RespostaConversa.comOpcoes(
                "Você digitou \"" + escaparHtml(interpretado.nomeRestante()) + "\". Temos " + opcaoOuOpcoes
                        + " em estoque:\n" + listarOpcoes(encontrados)
                        + "\n\nToque no número da opção para confirmar, ou em \"Cancelar\" para desistir.",
                opcoesEscolhaComCancelar(encontrados.size()));
    }

    private RespostaConversa tratarEscolhaMedicamento(String telefone, String texto, ConversaEstado conversaEstado) {
        List<Long> candidatos = conversaEstado.getCandidatosMedicamentoIds();
        Integer indiceEscolhido = tentarConverterParaIndice(texto, candidatos.size());
        if (indiceEscolhido == null) {
            return RespostaConversa.comOpcoes(
                    "Não entendi sua escolha. Responda com o número de uma das opções abaixo, "
                            + "ou toque em \"Cancelar\" para desistir:\n" + listarOpcoesPorIds(candidatos),
                    opcoesEscolhaComCancelar(candidatos.size()));
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

    private RespostaConversa adicionarItemEResponder(String telefone, ConversaEstado conversaEstado, Long pacienteId,
                                                      Medicamento medicamento, int quantidade) {
        try {
            Reserva reserva = adicionarItemReservaUseCase.executar(pacienteId, medicamento.getId(), quantidade);
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR);
            conversaEstado.setReservaRascunhoId(reserva.getId());
            conversaEstado.setCandidatosMedicamentoIds(List.of());
            conversaEstado.setQuantidadeDesejada(1);
            conversaEstadoGateway.salvar(conversaEstado);
            String prefixoQuantidade = quantidade > 1 ? quantidade + "x " : "";
            return RespostaConversa.comOpcoes(
                    "Você selecionou " + prefixoQuantidade + "<b>" + escaparHtml(medicamento.getNome())
                            + "</b>. Sua reserva atual:\n" + listarItensEmBullets(reserva)
                            + "\n\nDeseja reservar mais algum medicamento? Se sim, digite o nome dele. "
                            + "Ou toque em \"Finalizar\" para concluir a reserva.",
                    OPCOES_FINALIZAR_CANCELAR);
        } catch (EstoqueInsuficienteException e) {
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
            conversaEstado.setCandidatosMedicamentoIds(List.of());
            conversaEstado.setQuantidadeDesejada(1);
            conversaEstadoGateway.salvar(conversaEstado);
            return RespostaConversa.semOpcoes("No momento não há estoque disponível para \""
                    + escaparHtml(medicamento.getNome()) + "\" na quantidade solicitada. Informe o nome de outro "
                    + "medicamento, ou \"cancelar\" para desistir:");
        }
    }

    private RespostaConversa tratarMaisItensOuFinalizar(String telefone, String texto, ConversaEstado conversaEstado) {
        if (IntencaoMatcher.pareceRemocaoDeItem(texto)) {
            return tratarRemocaoUltimoItem(telefone, conversaEstado);
        }

        if (IntencaoMatcher.pareceFinalizacao(texto)) {
            Long reservaRascunhoId = conversaEstado.getReservaRascunhoId();
            Reserva reserva = reservaGateway.buscarPorId(reservaRascunhoId)
                    .orElseThrow(() -> new ReservaNaoEncontradaException(reservaRascunhoId));
            conversaEstadoGateway.remover(telefone);
            return RespostaConversa.semOpcoes("Reserva finalizada com sucesso! Itens reservados:\n"
                    + listarItensEmBullets(reserva) + "\n\nRetire seus medicamentos até "
                    + reserva.getDataExpiracao().format(FORMATO_DATA)
                    + ", após esse prazo a reserva será cancelada automaticamente.");
        }

        return tratarNovoItem(telefone, texto, conversaEstado);
    }

    private RespostaConversa tratarRemocaoUltimoItem(String telefone, ConversaEstado conversaEstado) {
        RemoverUltimoItemReservaUseCase.Resultado resultado =
                removerUltimoItemReservaUseCase.executar(conversaEstado.getReservaRascunhoId());

        if (resultado.reservaAtualizada().isEmpty()) {
            conversaEstado.setEstado(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
            conversaEstado.setReservaRascunhoId(null);
            conversaEstadoGateway.salvar(conversaEstado);
            return RespostaConversa.semOpcoes("Removi \"" + escaparHtml(resultado.itemRemovido().getNomeMedicamento())
                    + "\" da sua reserva. Como era o único item, cancelei a reserva. "
                    + "Se quiser começar outra, informe o nome de um medicamento:");
        }

        return RespostaConversa.comOpcoes(
                "Removi \"" + escaparHtml(resultado.itemRemovido().getNomeMedicamento())
                        + "\" da sua reserva. Itens restantes:\n"
                        + listarItensEmBullets(resultado.reservaAtualizada().get())
                        + "\n\nDeseja reservar mais algum medicamento? Se sim, digite o nome dele. "
                        + "Ou toque em \"Finalizar\" para concluir a reserva.",
                OPCOES_FINALIZAR_CANCELAR);
    }

    private RespostaConversa tratarConfirmacaoCancelamento(String telefone, String texto, ConversaEstado conversaEstado) {
        Boolean confirmou = IntencaoMatcher.interpretarConfirmacao(texto);
        if (confirmou == null && IntencaoMatcher.pareceCancelamentoTotal(texto)) {
            confirmou = Boolean.TRUE;
        }
        if (confirmou == null) {
            return RespostaConversa.comOpcoes(
                    "Não entendi. Você quer mesmo cancelar a reserva? Responda sim ou não.", OPCOES_SIM_NAO);
        }

        conversaEstadoGateway.remover(telefone);
        if (confirmou) {
            cancelarReservaUseCase.executar(conversaEstado.getReservaRascunhoId());
            return RespostaConversa.semOpcoes(
                    "Sua reserva foi cancelada com sucesso e os medicamentos foram liberados de volta ao estoque.");
        }
        return RespostaConversa.semOpcoes("Ok, sua reserva ativa foi mantida.");
    }

    private String listarItensEmBullets(Reserva reserva) {
        return reserva.getItens().stream()
                .map(item -> "• " + escaparHtml(item.getQuantidade() > 1
                        ? item.getNomeMedicamento() + " (x" + item.getQuantidade() + ")"
                        : item.getNomeMedicamento()))
                .collect(Collectors.joining("\n"));
    }

    private String listarOpcoes(List<Medicamento> medicamentos) {
        return IntStream.range(0, medicamentos.size())
                .mapToObj(indice -> (indice + 1) + ". " + escaparHtml(medicamentos.get(indice).getNome()))
                .collect(Collectors.joining("\n"));
    }

    private String listarOpcoesPorIds(List<Long> medicamentoIds) {
        return IntStream.range(0, medicamentoIds.size())
                .mapToObj(indice -> {
                    String nome = buscarMedicamentosUseCase.executarPorId(medicamentoIds.get(indice))
                            .map(Medicamento::getNome)
                            .orElse("opção indisponível");
                    return (indice + 1) + ". " + escaparHtml(nome);
                })
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

    private String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
