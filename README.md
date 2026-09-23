# 💊 FarmaSUS Digital — Reserva de Medicamentos via Telegram

<div align="center">
 <h2> Sumário</h2>
  <a href="#descrição-do-projeto">Descrição do projeto</a> -
  <a href="#arquitetura">Arquitetura</a> -
  <a href="#fluxo-de-atendimento-no-bot">Fluxo de atendimento no bot</a> -
  <a href="#regras-de-negócio">Regras de negócio</a> -
  <a href="#jobs-agendados">Jobs agendados</a> -
  <a href="#api-rest">API REST</a> -
  <a href="#ferramentas-utilizadas">Ferramentas utilizadas</a> -
  <a href="#guia-de-execução">Guia de execução</a> -
  <a href="#configuração">Configuração</a> -
  <a href="#documentação-da-api">Documentação da API</a> -
  <a href="#testes">Testes</a> -
  <a href="#desenvolvedores">Desenvolvedores</a>
</div>

## Descrição do projeto

<p align="justify">
Este projeto foi criado para a pós-graduação em Desenvolvimento e Arquitetura Java da instituição FIAP.

A aplicação é um <b>sistema de reserva de medicamentos do SUS</b>: o paciente conversa com um <b>bot do Telegram</b>, se cadastra pelo CPF, pesquisa medicamentos pelo nome e monta uma reserva com um ou mais itens. O estoque é <b>debitado no momento da reserva</b> e devolvido automaticamente caso ela seja cancelada ou expire. O paciente tem <b>1 dia útil</b> para retirar os medicamentos no posto de saúde e recebe um <b>lembrete</b> antes do prazo terminar.

Além do bot, o sistema expõe uma <b>API REST</b> para a equipe do posto: cadastro e consulta de estoque, consulta de pacientes e baixa das reservas (retirada ou cancelamento). O backend é um monólito <b>Spring Boot</b> construído em <b>Clean Architecture</b> (domain / application / infrastructure), com o domínio livre de qualquer dependência de framework.
</p>

## Arquitetura

O projeto segue **Clean Architecture**: as camadas de fora dependem das de dentro, nunca o contrário. O domínio não conhece Spring, JPA nem o Telegram — a comunicação com o mundo externo acontece por **gateways** (portas) implementados na infraestrutura.

```
br.com.fiap.farmasusdigital
├── domain/                             (regra de negócio pura, sem framework)
│   ├── model/         Reserva · ItemReserva · Medicamento · Paciente · ConversaEstado
│   ├── service/       CpfValidator · IntencaoMatcher · MedicamentoNomeMatcher
│   │                  NomeFormatter · ReservaExpiracaoPolicy
│   └── exception/     EstoqueInsuficiente · ReservaNaoEstaAtiva · ...
│
├── application/                        (casos de uso e portas)
│   ├── gateway/       ReservaGateway · MedicamentoGateway · PacienteGateway
│   │                  ConversaEstadoGateway · NotificacaoGateway
│   └── usecase/       ProcessarMensagemTelegramUseCase · AdicionarItemReservaUseCase
│                      CancelarReservaUseCase · RetirarReservaUseCase
│                      ExpirarReservasVencidasUseCase · EnviarLembretesExpiracaoUseCase · ...
│
└── infrastructure/                     (detalhes: frameworks e I/O)
    ├── rest/          Controllers · DTOs · ApiExceptionHandler
    ├── telegram/      Webhook · Polling · TelegramClient · UpdateHandler
    ├── persistence/   Entidades JPA · Repositories · implementações dos gateways
    ├── scheduler/     Jobs de expiração, lembrete e timeout de conversa
    └── config/        OpenApiConfig · ClockConfig
```

Visão do fluxo em tempo de execução:

```
                    mensagem / clique em botão
   [Paciente] ──────────────────────────────►  [ Telegram Bot API ]
        ▲                                               │
        │                                   webhook ou polling
        │                                               ▼
        │                              [ TelegramUpdateHandler ]
        │                                               │
        │  resposta do bot                              ▼
        └───────────────────────  [ ProcessarMensagemTelegramUseCase ]
                                        (máquina de estados)
                                                        │
                                                        ▼
                                          [ Casos de uso de reserva ]
                                                        │
                                                        ▼
                                              [ PostgreSQL ]
                                                        ▲
   [Posto de saúde] ───────────────────────────────────┘
        API REST: estoque · pacientes · retirar/cancelar reserva
```

| Camada | Responsabilidade |
|---|---|
| **domain** | Ciclo de vida da reserva, validação de CPF, política de expiração em dias úteis, interpretação de intenção e busca tolerante por nome de medicamento. |
| **application** | Casos de uso que orquestram o domínio e a máquina de estados da conversa; define as portas (gateways). |
| **infrastructure** | Controllers REST, integração com o Telegram, persistência JPA/PostgreSQL, jobs agendados e configuração. |

## Fluxo de atendimento no bot

A conversa é uma **máquina de estados** persistida por telefone (o `chat id` do Telegram), com três cenários de entrada:

| Cenário | O que o bot faz |
|---|---|
| **Paciente novo** | Pede o CPF, valida (dígitos verificadores), pede o nome completo e conclui o cadastro. |
| **Paciente sem reserva ativa** | Cumprimenta pelo nome e já pede o medicamento desejado. |
| **Paciente com reserva ativa** | Mostra os itens e a data de expiração, e pergunta se deseja cancelar a reserva. |

Estados da conversa: `AGUARDANDO_CPF` → `AGUARDANDO_NOME_PACIENTE` → `AGUARDANDO_NOME_MEDICAMENTO` → `AGUARDANDO_ESCOLHA_MEDICAMENTO` → `AGUARDANDO_MAIS_ITENS_OU_FINALIZAR` (e `AGUARDANDO_CONFIRMACAO_CANCELAMENTO`).

Comportamentos que tornam a conversa tolerante:

- **Busca flexível de medicamento:** ignora acentos, caixa, ordem das palavras e plural — `"dipironas 500"` encontra `Dipirona Sódica 500mg`. O resultado sempre vem como lista numerada para o paciente confirmar.
- **Quantidade no texto:** `"2 dipirona"` já reserva duas unidades.
- **Botões inline:** as opções (números, `Finalizar`, `Cancelar`, `Sim`/`Não`) aparecem como botões, mas digitar também funciona.
- **Intenções em linguagem natural:** finalizar, cancelar, remover o último item (`"remover esse"`), consultar o que já foi reservado (`"o que eu já reservei?"`), pedir ajuda e corrigir o CPF durante o cadastro.
- **Cancelar em qualquer etapa:** a conversa nunca vira um beco sem saída.
- **Timeout:** conversas paradas são encerradas automaticamente (padrão: 30 minutos).

## Regras de negócio

- **CPF válido** é obrigatório para o cadastro (validação de dígitos verificadores, aceita com ou sem pontuação).
- **Nome do paciente** é capitalizado automaticamente no cadastro.
- **Uma reserva ativa por paciente** — novos itens entram na reserva aberta em vez de criar outra.
- **Estoque debitado na reserva** e devolvido ao cancelar ou expirar; não é possível reservar acima do disponível.
- **Prazo de retirada:** 1 dia útil a partir da criação, pulando sábados e domingos.
- **Status da reserva:** `ATIVA` → `RETIRADA` · `CANCELADA` · `EXPIRADA`. Uma vez finalizada, não muda mais de estado.

## Jobs agendados

Três jobs em background (`@Scheduled`), isolados por item para que a falha de uma reserva não interrompa o lote:

| Job | Intervalo padrão | Responsabilidade |
|---|---|---|
| `ExpiracaoReservaScheduler` | 15 min | Expira reservas vencidas e devolve os itens ao estoque. |
| `LembreteExpiracaoScheduler` | 15 min | Avisa o paciente no Telegram antes de a reserva expirar (padrão: 1h de antecedência, uma vez por reserva). |
| `ConversaInativaScheduler` | 10 min | Encerra conversas do bot paradas por mais tempo que o timeout. |

## API REST

Base: `http://localhost:8080`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/estoque/medicamentos` | Lista os medicamentos e o estoque disponível. |
| `POST` | `/estoque/medicamentos` | Cadastra um medicamento (`nome`, `quantidadeEstoque`). |
| `GET` | `/pacientes/{identificador}` | Busca o paciente por CPF ou telefone. |
| `GET` | `/reservas/paciente/{pacienteId}` | Lista as reservas de um paciente. |
| `POST` | `/reservas/{id}/cancelar` | Cancela a reserva e devolve os itens ao estoque. |
| `POST` | `/reservas/{id}/retirar` | Dá baixa na retirada dos medicamentos. |
| `POST` | `/telegram/webhook` | Recebe os *updates* enviados pela Bot API do Telegram. |

Erros seguem um corpo único (`timestamp`, `status`, `mensagem`), traduzidos por um `@RestControllerAdvice`: `404` para recursos não encontrados, `400` para dados inválidos e `409` para conflitos de regra (estoque insuficiente, reserva já finalizada).

## Ferramentas utilizadas
<div style="display: flex; gap: 15px">
<a href="https://www.java.com" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" alt="Java" width="40" height="40"/>
</a>
<a href="https://spring.io/projects/spring-boot" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/spring/spring-original.svg" alt="Spring Boot" width="40" height="40"/>
</a>
<a href="https://www.postgresql.org/" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/postgresql/postgresql-original.svg" alt="PostgreSQL" width="40" height="40"/>
</a>
<a href="https://www.docker.com/" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/docker/docker-original.svg" alt="Docker" width="40" height="40"/>
</a>
<a href="https://core.telegram.org/bots/api" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/telegram/telegram-original.svg" alt="Telegram" width="40" height="40"/>
</a>
<a href="https://swagger.io/" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/swagger/swagger-original.svg" alt="Swagger" width="40" height="40"/>
</a>
<a href="https://maven.apache.org/" target="_blank">
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/apache/apache-original.svg" alt="Maven" width="40" height="40"/>
</a>
</div>

**Stack detalhada:** Java 21 · Spring Boot 4.1 (Web, Data JPA, Validation, Scheduling) · PostgreSQL · Hibernate · springdoc-openapi (Swagger UI) · Telegram Bot API · JUnit 5 · Mockito · H2 (testes) · JaCoCo · Maven · Docker.

## Guia de execução

### Pré-requisitos
- Java 21 e Maven
- PostgreSQL (ou Docker)
- Um bot criado no Telegram via [@BotFather](https://t.me/BotFather), para obter o token

### Clonar o repositório
```bash
git clone https://github.com/PedroNeves-git/FarmaSusDigital.git
cd FarmaSusDigital
```

### Subir o banco
```bash
docker run --name farmasus-db -e POSTGRES_DB=farmasus_digital \
  -e POSTGRES_USER=farmasus -e POSTGRES_PASSWORD=farmasus \
  -p 5432:5432 -d postgres:16
```

O schema é criado pelo Hibernate (`ddl-auto=update`) e o `data.sql` popula 10 medicamentos de exemplo na primeira subida.

### Rodar a aplicação
```bash
export TELEGRAM_BOT_TOKEN=<token-do-BotFather>
export TELEGRAM_POLLING_ENABLED=true   # uso local, sem URL pública
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

### Rodar com Docker
```bash
docker build -t farmasus-digital .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/farmasus_digital \
  -e TELEGRAM_BOT_TOKEN=<token-do-BotFather> \
  farmasus-digital
```

### Conectar o bot

Há duas formas de receber as mensagens — **use uma ou outra**, nunca as duas:

- **Polling** (local): `TELEGRAM_POLLING_ENABLED=true`. A aplicação consulta a API do Telegram periodicamente, sem precisar de URL pública.
- **Webhook** (produção): registre a URL pública no Telegram e mantenha o polling desligado.
  ```bash
  curl "https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://<sua-url>/telegram/webhook"
  ```

## Configuração

Todas as variáveis têm valor padrão para desenvolvimento local — apenas o token do bot é obrigatório.

| Variável | Padrão | Descrição |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | *(vazio)* | Token do bot gerado pelo BotFather. |
| `TELEGRAM_POLLING_ENABLED` | `false` | Habilita o polling como alternativa ao webhook. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/farmasus_digital` | URL de conexão do PostgreSQL. |
| `DB_USERNAME` | `farmasus` | Usuário do banco. |
| `DB_PASSWORD` | `farmasus` | Senha do banco. |
| `RESERVA_LEMBRETE_ANTECEDENCIA_HORAS` | `1` | Antecedência do lembrete de expiração. |
| `CONVERSA_TIMEOUT_MINUTOS` | `30` | Tempo de inatividade que encerra a conversa no bot. |

> O token **não fica no código** — é lido do ambiente pela aplicação.

## Documentação da API

- **Swagger UI:** `http://localhost:8080/swagger-ui.html` — todos os endpoints com exemplos pré-preenchidos, prontos para executar.
- **OpenAPI (JSON):** `http://localhost:8080/v3/api-docs`
- **Postman:** importe `postman/FarmaSUS-Digital.postman_collection.json`. A collection cobre estoque, pacientes, reservas e a simulação de mensagens do Telegram (texto digitado e clique em botão inline), sem precisar de um bot real.

## Testes

```bash
mvn test                      # executa a suíte
mvn test && open target/site/jacoco/index.html   # relatório de cobertura (JaCoCo)
```

A suíte cobre o domínio (ciclo de vida da reserva, estoque, validação de CPF, matchers de nome e intenção, formatação de nome) e os casos de uso (máquina de estados da conversa, adição e remoção de itens, cancelamento, expiração e lembretes), com `Clock` fixo para tornar as regras de data determinísticas.

## Desenvolvedores
<table align="center">
  <tr>
    <td align="center">
      <div>
        <img src="https://avatars.githubusercontent.com/PedroNeves-git" width="120px;" alt="Foto no GitHub" class="profile"/><br>
          <b> Pedro Neves   </b><br>
            <a href="https://www.linkedin.com/in/pedro-neves-867001258/" alt="Linkedin"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20"></a>
            <a href="https://github.com/PedroNeves-git" alt="Github"><img src="https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white" height="20"></a>
      </div>
    </td>
    <td align="center">
      <div>
        <img src="https://avatars.githubusercontent.com/breenoox" width="120px;" alt="Foto no GitHub" class="profile"/><br>
          <b> Breno Barbosa   </b><br>
            <a href="https://www.linkedin.com/in/brenobarbosa22/" alt="Linkedin"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20"></a>
            <a href="https://github.com/breenoox" alt="Github"><img src="https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white" height="20"></a>
      </div>
    </td>
    <td align="center">
      <div>
        <img src="https://avatars.githubusercontent.com/GuiFonsCode" width="120px;" alt="Foto no GitHub" class="profile"/><br>
          <b> Guilherme Fonseca   </b><br>
            <a href="https://www.linkedin.com/in/guifonseca1212/" alt="Linkedin"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20"></a>
            <a href="https://github.com/GuiFonsCode" alt="Github"><img src="https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white" height="20"></a>
      </div>
    </td>
  </tr>
</table>
