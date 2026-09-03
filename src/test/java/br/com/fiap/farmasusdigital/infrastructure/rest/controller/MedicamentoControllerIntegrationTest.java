package br.com.fiap.farmasusdigital.infrastructure.rest.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MedicamentoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCadastrarEComListarMedicamento() throws Exception {
        mockMvc.perform(post("/estoque/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Paracetamol 750mg\",\"quantidadeEstoque\":42}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Paracetamol 750mg"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(42));

        mockMvc.perform(get("/estoque/medicamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void deveRetornarBadRequestQuandoNomeNaoInformado() throws Exception {
        mockMvc.perform(post("/estoque/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"quantidadeEstoque\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarNotFoundParaPacienteInexistente() throws Exception {
        mockMvc.perform(get("/pacientes/00000000000"))
                .andExpect(status().isNotFound());
    }
}
