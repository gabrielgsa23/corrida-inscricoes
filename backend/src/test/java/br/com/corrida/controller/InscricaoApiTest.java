package br.com.corrida.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.corrida.repository.InscricaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class InscricaoApiTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private InscricaoRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void comecaVaziaComResumoZerado() throws Exception {
        mvc.perform(get("/api/inscricoes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mvc.perform(get("/api/inscricoes/resumo"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"total\": 0, \"km5\": 0, \"km10\": 0}"));
    }

    @Test
    void cadastraNormalizandoECalculandoIdadeECategoria() throws Exception {
        cadastrar("  Ana   Souza ", " Ana.Souza@Email.COM ", "1990-05-20", "KM_10")
                .andExpect(status().isCreated())
                .andExpect(content().json("""
                        {
                          "nome": "Ana Souza",
                          "email": "ana.souza@email.com",
                          "dataNascimento": "1990-05-20",
                          "percurso": "KM_10",
                          "idadeNaProva": 36,
                          "categoria": "ADULTO"
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void recusaEmailRepetidoMesmoComCaixaEEspacosDiferentes() throws Exception {
        cadastrar("Ana Souza", "ana.souza@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());

        cadastrar("Outra Ana", "  ANA.Souza@email.com ", "1991-01-01", "KM_5")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma inscrição ativa para este e-mail."));
    }

    @Test
    void recusaMenorDe14ComAMensagemDoContrato() throws Exception {
        cadastrar("Júlia Marta", "julia@email.com", "2014-06-01", "KM_10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A idade mínima para participar é 14 anos."));
    }

    @Test
    void recusa29DeFevereiroEmAnoNaoBissextoComMensagemPropria() throws Exception {
        cadastrar("Júlia Marta", "julia@email.com", "2001-02-29", "KM_5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A data 29/02 só existe em ano bissexto, e 2001 não é bissexto."));
    }

    @Test
    void listaNaOrdemDeCadastroPreservandoAcentos() throws Exception {
        String nomeLongo = "Maria da Conceição D'Ávila-Gonçalves " + "Ç".repeat(60);
        cadastrar(nomeLongo, "maria@email.com", "1960-01-10", "KM_5");
        cadastrar("João Pedro", "joao+corrida@gmail.com", "2000-03-14", "KM_10");

        mvc.perform(get("/api/inscricoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value(nomeLongo))
                .andExpect(jsonPath("$[0].categoria").value("SENIOR"))
                .andExpect(jsonPath("$[1].email").value("joao+corrida@gmail.com"));

        mvc.perform(get("/api/inscricoes/joao+corrida@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idadeNaProva").value(27));
    }

    @Test
    void editaMantendoEmailEPosicaoEMoveOResumo() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_10");
        cadastrar("Bruno Lima", "bruno@email.com", "1985-01-01", "KM_5");

        editar("ANA@email.com", "Ana Maria Souza", "outro@email.com", "1987-03-14", "KM_5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.nome").value("Ana Maria Souza"))
                .andExpect(jsonPath("$.idadeNaProva").value(40))
                .andExpect(jsonPath("$.categoria").value("MASTER"));

        mvc.perform(get("/api/inscricoes"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("ana@email.com"));

        mvc.perform(get("/api/inscricoes/resumo"))
                .andExpect(content().json("{\"total\": 2, \"km5\": 2, \"km10\": 0}"));
    }

    @Test
    void edicaoRecusadaNaoAlteraNada() throws Exception {
        cadastrar("Bruno Lima", "bruno@email.com", "2009-03-15", "KM_5");

        editar("bruno@email.com", "Bruno Alterado", "bruno@email.com", "2009-03-15", "KM_10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O percurso de 10 km é permitido somente a partir de 18 anos."));

        mvc.perform(get("/api/inscricoes/bruno@email.com"))
                .andExpect(jsonPath("$.nome").value("Bruno Lima"))
                .andExpect(jsonPath("$.percurso").value("KM_5"));

        mvc.perform(get("/api/inscricoes/resumo"))
                .andExpect(content().json("{\"total\": 1, \"km5\": 1, \"km10\": 0}"));
    }

    @Test
    void cancelaLiberandoOEmailERecadastroVaiParaOFim() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5");
        cadastrar("Bruno Lima", "bruno@email.com", "1985-01-01", "KM_5");

        mvc.perform(delete("/api/inscricoes/ana@email.com"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(delete("/api/inscricoes/ana@email.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Não existe inscrição ativa para este e-mail."));

        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_10").andExpect(status().isCreated());

        mvc.perform(get("/api/inscricoes"))
                .andExpect(jsonPath("$[0].email").value("bruno@email.com"))
                .andExpect(jsonPath("$[1].email").value("ana@email.com"));
    }

    @Test
    void emailInexistenteDevolve404NaConsultaENaEdicao() throws Exception {
        mvc.perform(get("/api/inscricoes/ninguem@email.com"))
                .andExpect(status().isNotFound());

        editar("ninguem@email.com", "Ninguém", "", "1990-05-20", "KM_5")
                .andExpect(status().isNotFound());
    }

    @Test
    void jsonQuebradoERotaInexistenteNuncaDevolvem500() throws Exception {
        mvc.perform(post("/api/inscricoes").contentType(MediaType.APPLICATION_JSON).content("{\"nome\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O corpo da requisição está vazio ou não é um JSON válido."));

        mvc.perform(get("/api/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Rota não encontrada."));
    }

    @Test
    void numeroOuListaNoLugarDeTextoNuncaDevolvem500() throws Exception {
        enviar("{\"nome\": 123, \"email\": \"ana@email.com\", \"dataNascimento\": \"1990-05-20\", \"percurso\": \"KM_5\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O nome deve conter apenas letras, espaços, apóstrofos e hífens."));

        enviar("{\"nome\": [1, 2], \"email\": \"ana@email.com\", \"dataNascimento\": \"1990-05-20\", \"percurso\": \"KM_5\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O corpo da requisição está vazio ou não é um JSON válido."));
    }

    @Test
    void camposAMaisNoJsonSaoIgnoradosEIdadeECategoriaSaoSempreCalculadas() throws Exception {
        enviar("{\"nome\": \"Ana Souza\", \"email\": \"ana@email.com\", \"dataNascimento\": \"1990-05-20\","
                + " \"percurso\": \"KM_5\", \"categoria\": \"JUVENIL\", \"idadeNaProva\": 5, \"id\": 99}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("ADULTO"))
                .andExpect(jsonPath("$.idadeNaProva").value(36));
    }

    @Test
    void corpoNullDevolve400() throws Exception {
        enviar("null").andExpect(status().isBadRequest());
    }

    private ResultActions cadastrar(String nome, String email, String nascimento, String percurso) throws Exception {
        return mvc.perform(post("/api/inscricoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(nome, email, nascimento, percurso)));
    }

    private ResultActions editar(String emailDaUrl, String nome, String emailNoCorpo, String nascimento,
                                 String percurso) throws Exception {
        return mvc.perform(put("/api/inscricoes/" + emailDaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(nome, emailNoCorpo, nascimento, percurso)));
    }

    private ResultActions enviar(String corpo) throws Exception {
        return mvc.perform(post("/api/inscricoes").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String json(String nome, String email, String nascimento, String percurso) {
        return """
                {"nome": "%s", "email": "%s", "dataNascimento": "%s", "percurso": "%s"}
                """.formatted(nome, email, nascimento, percurso);
    }
}
