package br.com.fiap.farmasusdigital.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.farmasusdigital.application.gateway.ConversaEstadoGateway;

@ExtendWith(MockitoExtension.class)
class ExpirarConversasInativasUseCaseTest {

    @Mock
    private ConversaEstadoGateway conversaEstadoGateway;

    private ExpirarConversasInativasUseCase useCase;

    private final Clock clockFixo = Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        useCase = new ExpirarConversasInativasUseCase(conversaEstadoGateway, clockFixo, 30L);
    }

    @Test
    void deveRemoverConversasInativasAntesDoLimiteDeTimeout() {
        when(conversaEstadoGateway.removerInativasAntesDe(any())).thenReturn(3);

        int quantidade = useCase.executar();

        assertThat(quantidade).isEqualTo(3);
        ArgumentCaptor<java.time.LocalDateTime> captor = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        verify(conversaEstadoGateway).removerInativasAntesDe(captor.capture());
        assertThat(captor.getValue()).isEqualTo(java.time.LocalDateTime.now(clockFixo).minusMinutes(30));
    }
}
