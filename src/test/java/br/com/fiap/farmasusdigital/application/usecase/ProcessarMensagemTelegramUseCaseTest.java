package br.com.fiap.farmasusdigital.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.farmasusdigital.application.gateway.ConversaEstadoGateway;
import br.com.fiap.farmasusdigital.application.gateway.ReservaGateway;
import br.com.fiap.farmasusdigital.domain.exception.CpfInvalidoException;
import br.com.fiap.farmasusdigital.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.farmasusdigital.domain.model.ConversaEstado;
import br.com.fiap.farmasusdigital.domain.model.EstadoConversa;
import br.com.fiap.farmasusdigital.domain.model.ItemReserva;
import br.com.fiap.farmasusdigital.domain.model.Medicamento;
import br.com.fiap.farmasusdigital.domain.model.Paciente;
import br.com.fiap.farmasusdigital.domain.model.Reserva;
import br.com.fiap.farmasusdigital.domain.model.ReservaStatus;

@ExtendWith(MockitoExtension.class)
class ProcessarMensagemTelegramUseCaseTest {

    @Mock
    private ConversaEstadoGateway conversaEstadoGateway;

    @Mock
    private ReservaGateway reservaGateway;

    @Mock
    private BuscarPacienteUseCase buscarPacienteUseCase;

    @Mock
    private CadastrarPacienteUseCase cadastrarPacienteUseCase;

    @Mock
    private BuscarMedicamentosUseCase buscarMedicamentosUseCase;

    @Mock
    private AdicionarItemReservaUseCase adicionarItemReservaUseCase;

    @Mock
    private RemoverUltimoItemReservaUseCase removerUltimoItemReservaUseCase;

    @Mock
    private CancelarReservaUseCase cancelarReservaUseCase;

    private ProcessarMensagemTelegramUseCase useCase;

    private static final String TELEFONE = "123456";
    private static final Paciente PACIENTE = new Paciente(1L, "52998224725", "Maria Silva", TELEFONE);

    @BeforeEach
    void setUp() {
        useCase = new ProcessarMensagemTelegramUseCase(conversaEstadoGateway, reservaGateway, buscarPacienteUseCase,
                cadastrarPacienteUseCase, buscarMedicamentosUseCase, adicionarItemReservaUseCase,
                removerUltimoItemReservaUseCase, cancelarReservaUseCase);
    }

    @Test
    void cenario1_devePedirCadastroParaNovoUsuario() {
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.empty());
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.empty());

        RespostaConversa resposta = useCase.executar(TELEFONE, "oi");

        assertThat(resposta.mensagem()).contains("Seja bem-vindo ao serviço de reservas");
        ArgumentCaptor<ConversaEstado> captor = ArgumentCaptor.forClass(ConversaEstado.class);
        verify(conversaEstadoGateway).salvar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_CPF);
    }

    @Test
    void cenario2_devePedirMedicamentoParaUsuarioSemReservaAtiva() {
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.empty());
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));
        when(reservaGateway.buscarAtivaPorPaciente(1L)).thenReturn(Optional.empty());

        RespostaConversa resposta = useCase.executar(TELEFONE, "oi");

        assertThat(resposta.mensagem()).contains("Seja bem-vindo, <b>Maria Silva</b>");
        assertThat(resposta.mensagem()).contains("Informe o nome de qual medicamento");
        ArgumentCaptor<ConversaEstado> captor = ArgumentCaptor.forClass(ConversaEstado.class);
        verify(conversaEstadoGateway).salvar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
    }

    @Test
    void cenario3_devePerguntarSobreCancelamentoParaUsuarioComReservaAtiva() {
        Reserva reservaAtiva = new Reserva(77L, 1L, List.of(new ItemReserva(1L, 10L, "Dipirona", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);

        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.empty());
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));
        when(reservaGateway.buscarAtivaPorPaciente(1L)).thenReturn(Optional.of(reservaAtiva));

        RespostaConversa resposta = useCase.executar(TELEFONE, "oi");

        assertThat(resposta.mensagem()).contains("reserva ativa dos itens:");
        assertThat(resposta.mensagem()).contains("• Dipirona");
        assertThat(resposta.mensagem()).contains("Ela expira em");
        assertThat(resposta.mensagem()).contains("Deseja cancelar esta reserva?");
        assertThat(resposta.opcoesRapidas()).containsExactly("Sim", "Não");
        ArgumentCaptor<ConversaEstado> captor = ArgumentCaptor.forClass(ConversaEstado.class);
        verify(conversaEstadoGateway).salvar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO);
        assertThat(captor.getValue().getReservaRascunhoId()).isEqualTo(77L);
    }

    @Test
    void devePedirNomeApósReceberCpfValido() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CPF, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "529.982.247-25");

        assertThat(resposta.mensagem()).contains("CPF válido");
        assertThat(resposta.mensagem()).contains("nome completo");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_NOME_PACIENTE);
        assertThat(estado.getCpfTemporario()).isEqualTo("52998224725");
        verify(cadastrarPacienteUseCase, never()).executar(any(), any(), any());
    }

    @Test
    void devePedirCpfNovamenteQuandoInvalido() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CPF, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "111.111.111-11");

        assertThat(resposta.mensagem()).contains("não parece válido");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_CPF);
        verify(conversaEstadoGateway, never()).salvar(any());
    }

    @Test
    void deveSempreOferecerOpcaoDeCancelarQuandoCpfInvalido() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CPF, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "111.111.111-11");

        assertThat(resposta.mensagem()).contains("cancelar");
    }

    @Test
    void devePermitirCancelarNoMeioDoCadastroAguardandoCpf() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CPF, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "cancelar");

        assertThat(resposta.mensagem()).contains("cadastro cancelado");
        verify(conversaEstadoGateway).remover(TELEFONE);
    }

    @Test
    void devePermitirCancelarNoMeioDoCadastroAguardandoNome() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_PACIENTE, null);
        estado.setCpfTemporario("52998224725");
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "desisti, esquece");

        assertThat(resposta.mensagem()).contains("cadastro cancelado");
        verify(conversaEstadoGateway).remover(TELEFONE);
        verify(cadastrarPacienteUseCase, never()).executar(any(), any(), any());
    }

    @Test
    void deveCadastrarPacienteQuandoNomeCompletoInformadoAposCpf() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_PACIENTE, null);
        estado.setCpfTemporario("52998224725");
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(cadastrarPacienteUseCase.executar(eq("52998224725"), eq("Maria Silva"), eq(TELEFONE)))
                .thenReturn(PACIENTE);

        RespostaConversa resposta = useCase.executar(TELEFONE, "Maria Silva");

        assertThat(resposta.mensagem()).contains("Cadastro realizado com sucesso, <b>Maria Silva</b>");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
        assertThat(estado.getCpfTemporario()).isNull();
    }

    @Test
    void devePedirNomeCompletoNovamenteQuandoApenasUmaPalavra() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_PACIENTE, null);
        estado.setCpfTemporario("52998224725");
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "Maria");

        assertThat(resposta.mensagem()).contains("nome completo");
        verify(cadastrarPacienteUseCase, never()).executar(any(), any(), any());
    }

    @Test
    void devePedirCpfNovamenteQuandoCadastroFalhaComCpfInvalido() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_PACIENTE, null);
        estado.setCpfTemporario("52998224725");
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(cadastrarPacienteUseCase.executar(any(), any(), any()))
                .thenThrow(new CpfInvalidoException("52998224725"));

        RespostaConversa resposta = useCase.executar(TELEFONE, "Maria Silva");

        assertThat(resposta.mensagem()).contains("comece novamente");
        verify(conversaEstadoGateway).remover(TELEFONE);
    }

    @Test
    void deveListarOpcaoUnicaEAguardarConfirmacaoQuandoApenasUmMedicamentoCorresponde() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento dipirona = new Medicamento(10L, "Dipirona Sódica 500mg", 5);
        when(buscarMedicamentosUseCase.executarPorNomeSemelhante("dipirona")).thenReturn(List.of(dipirona));

        RespostaConversa resposta = useCase.executar(TELEFONE, "dipirona");

        assertThat(resposta.mensagem()).contains("Você digitou \"dipirona\"");
        assertThat(resposta.mensagem()).contains("a seguinte opção");
        assertThat(resposta.mensagem()).contains("1. Dipirona Sódica 500mg");
        assertThat(resposta.opcoesRapidas()).containsExactly("1", "Cancelar");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO);
        assertThat(estado.getCandidatosMedicamentoIds()).containsExactly(10L);
        verify(adicionarItemReservaUseCase, never()).executar(any(), any(), anyInt());
    }

    @Test
    void deveAdicionarAposConfirmarOpcaoUnicaListada() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO, null,
                List.of(10L));
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento dipirona = new Medicamento(10L, "Dipirona Sódica 500mg", 5);
        when(buscarMedicamentosUseCase.executarPorId(10L)).thenReturn(Optional.of(dipirona));

        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.setId(50L);
        reserva.adicionarItem(new ItemReserva(1L, 10L, "Dipirona Sódica 500mg", 1));
        when(adicionarItemReservaUseCase.executar(eq(1L), eq(10L), anyInt())).thenReturn(reserva);

        RespostaConversa resposta = useCase.executar(TELEFONE, "1");

        assertThat(resposta.mensagem()).contains("Dipirona Sódica 500mg");
        assertThat(resposta.mensagem()).contains("Você selecionou");
        assertThat(resposta.mensagem()).contains("Sua reserva atual");
        assertThat(resposta.mensagem()).contains("Finalizar");
        assertThat(resposta.opcoesRapidas()).containsExactly("Finalizar", "Cancelar");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR);
        assertThat(estado.getReservaRascunhoId()).isEqualTo(50L);
    }

    @Test
    void devePedirNovoNomeQuandoNenhumMedicamentoCorresponde() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));
        when(buscarMedicamentosUseCase.executarPorNomeSemelhante("xpto")).thenReturn(List.of());

        RespostaConversa resposta = useCase.executar(TELEFONE, "xpto");

        assertThat(resposta.mensagem()).contains("Não encontramos nenhum medicamento parecido");
        verify(adicionarItemReservaUseCase, never()).executar(any(), any(), anyInt());
    }

    @Test
    void devePedirEscolhaQuandoMaisDeUmMedicamentoCorresponde() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento opcao1 = new Medicamento(10L, "Dipirona Sódica 500mg", 5);
        Medicamento opcao2 = new Medicamento(11L, "Dipirona Monoidratada 1g", 5);
        when(buscarMedicamentosUseCase.executarPorNomeSemelhante("dipirona")).thenReturn(List.of(opcao1, opcao2));

        RespostaConversa resposta = useCase.executar(TELEFONE, "dipirona");

        assertThat(resposta.mensagem()).contains("Você digitou \"dipirona\"");
        assertThat(resposta.mensagem()).contains("as seguintes opções");
        assertThat(resposta.mensagem()).contains("1. Dipirona Sódica 500mg");
        assertThat(resposta.mensagem()).contains("2. Dipirona Monoidratada 1g");
        assertThat(resposta.opcoesRapidas()).containsExactly("1", "2", "Cancelar");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO);
        assertThat(estado.getCandidatosMedicamentoIds()).containsExactly(10L, 11L);
        verify(adicionarItemReservaUseCase, never()).executar(any(), any(), anyInt());
    }

    @Test
    void deveAdicionarMedicamentoEscolhidoPorNumero() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO, null,
                List.of(10L, 11L));
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento escolhido = new Medicamento(11L, "Dipirona Monoidratada 1g", 5);
        when(buscarMedicamentosUseCase.executarPorId(11L)).thenReturn(Optional.of(escolhido));

        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.setId(50L);
        when(adicionarItemReservaUseCase.executar(eq(1L), eq(11L), anyInt())).thenReturn(reserva);

        RespostaConversa resposta = useCase.executar(TELEFONE, "2");

        assertThat(resposta.mensagem()).contains("Dipirona Monoidratada 1g");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR);
    }

    @Test
    void devePedirNovamenteQuandoEscolhaForInvalida() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO, null,
                List.of(10L, 11L));
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarMedicamentosUseCase.executarPorId(10L))
                .thenReturn(Optional.of(new Medicamento(10L, "Dipirona Sódica 500mg", 5)));
        when(buscarMedicamentosUseCase.executarPorId(11L))
                .thenReturn(Optional.of(new Medicamento(11L, "Dipirona Monoidratada 1g", 5)));

        RespostaConversa resposta = useCase.executar(TELEFONE, "9");

        assertThat(resposta.mensagem()).contains("Não entendi sua escolha");
        assertThat(resposta.mensagem()).contains("1. Dipirona Sódica 500mg");
        assertThat(resposta.mensagem()).contains("2. Dipirona Monoidratada 1g");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO);
        verify(adicionarItemReservaUseCase, never()).executar(any(), any(), anyInt());
    }

    @Test
    void devePedirOutroMedicamentoQuandoEstoqueInsuficiente() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO, null,
                List.of(10L));
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento semEstoque = new Medicamento(10L, "Dipirona Sódica 500mg", 5);
        when(buscarMedicamentosUseCase.executarPorId(10L)).thenReturn(Optional.of(semEstoque));
        when(adicionarItemReservaUseCase.executar(eq(1L), eq(10L), anyInt()))
                .thenThrow(new EstoqueInsuficienteException("Dipirona Sódica 500mg"));

        RespostaConversa resposta = useCase.executar(TELEFONE, "1");

        assertThat(resposta.mensagem()).contains("não há estoque disponível");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
    }

    @Test
    void deveFinalizarReservaQuandoUsuarioNaoQuiserMaisItens() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        Reserva reserva = new Reserva(50L, 1L, List.of(new ItemReserva(1L, 10L, "Dipirona", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        when(reservaGateway.buscarPorId(50L)).thenReturn(Optional.of(reserva));

        RespostaConversa resposta = useCase.executar(TELEFONE, "finalizar");

        assertThat(resposta.mensagem()).contains("Reserva finalizada com sucesso");
        verify(conversaEstadoGateway).remover(TELEFONE);
    }

    @Test
    void deveFinalizarReservaComFraseLivreNaoApenasPalavraExata() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        Reserva reserva = new Reserva(50L, 1L, List.of(new ItemReserva(1L, 10L, "Dipirona", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        when(reservaGateway.buscarPorId(50L)).thenReturn(Optional.of(reserva));

        RespostaConversa resposta = useCase.executar(TELEFONE, "desejo finalizar a reserva");

        assertThat(resposta.mensagem()).contains("Reserva finalizada com sucesso");
        verify(conversaEstadoGateway).remover(TELEFONE);
    }

    @Test
    void deveFinalizarReservaQuandoUsuarioDizConfirmar() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        Reserva reserva = new Reserva(50L, 1L, List.of(new ItemReserva(1L, 10L, "Dipirona", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        when(reservaGateway.buscarPorId(50L)).thenReturn(Optional.of(reserva));

        RespostaConversa resposta = useCase.executar(TELEFONE, "pode confirmar");

        assertThat(resposta.mensagem()).contains("Reserva finalizada com sucesso");
    }

    @Test
    void deveRemoverUltimoItemQuandoPacienteExpressaEssaIntencao() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        ItemReserva itemRemovido = new ItemReserva(1L, 10L, "Dipirona Sódica 500mg", 1);
        Reserva reservaAtualizada = new Reserva(50L, 1L, List.of(new ItemReserva(2L, 20L, "Losartana 50mg", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        when(removerUltimoItemReservaUseCase.executar(50L))
                .thenReturn(new RemoverUltimoItemReservaUseCase.Resultado(itemRemovido, Optional.of(reservaAtualizada)));

        RespostaConversa resposta = useCase.executar(TELEFONE, "gostaria de remover esse da reserva");

        assertThat(resposta.mensagem()).contains("Removi \"Dipirona Sódica 500mg\"");
        assertThat(resposta.mensagem()).contains("Losartana 50mg");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR);
        verify(conversaEstadoGateway, never()).remover(any());
    }

    @Test
    void deveCancelarReservaInteiraQuandoRemoverUltimoItemRestante() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        ItemReserva itemRemovido = new ItemReserva(1L, 10L, "Dipirona Sódica 500mg", 1);
        when(removerUltimoItemReservaUseCase.executar(50L))
                .thenReturn(new RemoverUltimoItemReservaUseCase.Resultado(itemRemovido, Optional.empty()));

        RespostaConversa resposta = useCase.executar(TELEFONE, "tira esse");

        assertThat(resposta.mensagem()).contains("cancelei a reserva");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO);
        assertThat(estado.getReservaRascunhoId()).isNull();
    }

    @Test
    void devePedirConfirmacaoDeCancelamentoQuandoPacienteDesisteNoMeioDaConstrucao() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        Reserva reserva = new Reserva(50L, 1L, List.of(new ItemReserva(1L, 10L, "Dipirona", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        when(reservaGateway.buscarPorId(50L)).thenReturn(Optional.of(reserva));

        RespostaConversa resposta = useCase.executar(TELEFONE, "cancela tudo, desisti");

        assertThat(resposta.mensagem()).contains("tem certeza que deseja cancelar");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO);
        assertThat(estado.getReservaRascunhoId()).isEqualTo(50L);
        verify(conversaEstadoGateway, never()).remover(any());
    }

    @Test
    void deveEncerrarAtendimentoQuandoDesisteAntesDeAdicionarQualquerItem() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "ah esquece, desisti");

        assertThat(resposta.mensagem()).contains("encerrei o atendimento");
        verify(conversaEstadoGateway).remover(TELEFONE);
        verify(buscarMedicamentosUseCase, never()).executarPorNomeSemelhante(any());
    }

    @Test
    void devePedirConfirmacaoNovamenteQuandoRespostaForAmbigua() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO, 77L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "talvez");

        assertThat(resposta.mensagem()).contains("Não entendi");
        verify(conversaEstadoGateway, never()).remover(any());
        verify(cancelarReservaUseCase, never()).executar(any());
    }

    @Test
    void devePermitirConfirmarCancelamentoDizendoCancelar() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO, 77L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "cancelar");

        assertThat(resposta.mensagem()).contains("reserva foi cancelada com sucesso");
        verify(cancelarReservaUseCase).executar(77L);
    }

    @Test
    void deveCancelarReservaQuandoUsuarioConfirmar() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO, 77L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "sim");

        assertThat(resposta.mensagem()).contains("reserva foi cancelada com sucesso");
        verify(cancelarReservaUseCase, times(1)).executar(77L);
        verify(conversaEstadoGateway).remover(TELEFONE);
    }

    @Test
    void deveManterReservaQuandoUsuarioNaoConfirmarCancelamento() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_CONFIRMACAO_CANCELAMENTO, 77L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "não");

        assertThat(resposta.mensagem()).contains("reserva ativa foi mantida");
        verify(cancelarReservaUseCase, never()).executar(any());
        verify(conversaEstadoGateway).remover(TELEFONE);
    }

    @Test
    void deveResponderAjudaSemAlterarOEstadoAtual() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "ajuda");

        assertThat(resposta.mensagem()).contains("Finalizar");
        assertThat(resposta.opcoesRapidas()).containsExactly("Finalizar", "Cancelar");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR);
        verify(conversaEstadoGateway, never()).salvar(any());
        verify(conversaEstadoGateway, never()).remover(any());
    }

    @Test
    void deveMostrarItensJaReservadosSemAlterarEstado() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR, 50L);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        Reserva reserva = new Reserva(50L, 1L, List.of(new ItemReserva(1L, 10L, "Dipirona Sódica 500mg", 1)),
                ReservaStatus.ATIVA, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        when(reservaGateway.buscarPorId(50L)).thenReturn(Optional.of(reserva));

        RespostaConversa resposta = useCase.executar(TELEFONE, "o que eu já reservei?");

        assertThat(resposta.mensagem()).contains("Dipirona Sódica 500mg");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_MAIS_ITENS_OU_FINALIZAR);
        verify(conversaEstadoGateway, never()).salvar(any());
    }

    @Test
    void devePermitirCorrigirCpfEnquantoAguardaNomeDoPaciente() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_PACIENTE, null);
        estado.setCpfTemporario("52998224725");
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));

        RespostaConversa resposta = useCase.executar(TELEFONE, "errei o cpf, quero corrigir");

        assertThat(resposta.mensagem()).contains("Informe seu CPF novamente");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_CPF);
        assertThat(estado.getCpfTemporario()).isNull();
        verify(cadastrarPacienteUseCase, never()).executar(any(), any(), any());
    }

    @Test
    void deveGuardarQuantidadeInformadaAntesDoNomeAoListarOpcao() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_NOME_MEDICAMENTO, null);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento dipirona = new Medicamento(10L, "Dipirona Sódica 500mg", 5);
        when(buscarMedicamentosUseCase.executarPorNomeSemelhante("dipironas")).thenReturn(List.of(dipirona));

        RespostaConversa resposta = useCase.executar(TELEFONE, "2 dipironas");

        assertThat(resposta.mensagem()).contains("Dipirona Sódica 500mg");
        assertThat(estado.getEstado()).isEqualTo(EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO);
        assertThat(estado.getQuantidadeDesejada()).isEqualTo(2);
    }

    @Test
    void deveReservarQuantidadeInformadaAposConfirmarOpcaoUnica() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO, null,
                List.of(10L));
        estado.setQuantidadeDesejada(2);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento dipirona = new Medicamento(10L, "Dipirona Sódica 500mg", 5);
        when(buscarMedicamentosUseCase.executarPorId(10L)).thenReturn(Optional.of(dipirona));

        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.setId(50L);
        when(adicionarItemReservaUseCase.executar(eq(1L), eq(10L), eq(2))).thenReturn(reserva);

        RespostaConversa resposta = useCase.executar(TELEFONE, "1");

        assertThat(resposta.mensagem()).contains("2x");
        assertThat(resposta.mensagem()).contains("Dipirona Sódica 500mg");
    }

    @Test
    void deveAplicarQuantidadeInformadaMesmoAposEscolherEntreVariosMedicamentos() {
        ConversaEstado estado = new ConversaEstado(TELEFONE, EstadoConversa.AGUARDANDO_ESCOLHA_MEDICAMENTO, null,
                List.of(10L, 11L));
        estado.setQuantidadeDesejada(3);
        when(conversaEstadoGateway.buscarPorTelefone(TELEFONE)).thenReturn(Optional.of(estado));
        when(buscarPacienteUseCase.executarPorTelefone(TELEFONE)).thenReturn(Optional.of(PACIENTE));

        Medicamento escolhido = new Medicamento(11L, "Dipirona Monoidratada 1g", 5);
        when(buscarMedicamentosUseCase.executarPorId(11L)).thenReturn(Optional.of(escolhido));

        Reserva reserva = Reserva.abrirNova(1L, LocalDateTime.now());
        reserva.setId(50L);
        when(adicionarItemReservaUseCase.executar(eq(1L), eq(11L), eq(3))).thenReturn(reserva);

        RespostaConversa resposta = useCase.executar(TELEFONE, "2");

        assertThat(resposta.mensagem()).contains("3x");
    }
}
