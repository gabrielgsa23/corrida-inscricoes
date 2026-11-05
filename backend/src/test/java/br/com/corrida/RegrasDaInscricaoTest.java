package br.com.corrida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.corrida.RegrasDaInscricao.Categoria;
import br.com.corrida.RegrasDaInscricao.DadosValidos;
import br.com.corrida.RegrasDaInscricao.Percurso;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes das regras puras, sem Spring: cada teste roda em milissegundos.
 *
 * A data da prova e o "hoje" ficam fixos AQUI, de propósito. Se alguém mudar o
 * application.properties, estes testes continuam conferindo as bordas do regulamento.
 */
class RegrasDaInscricaoTest {

    private static final LocalDate PROVA = LocalDate.of(2027, 3, 14);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    // "Hoje" congelado em 04/11/2026, para o teste não mudar de resultado com o passar dos dias.
    private static final Clock HOJE_FIXO = Clock.fixed(
            LocalDate.of(2026, 11, 4).atStartOfDay(FUSO).toInstant(), FUSO);

    private final RegrasDaInscricao regras = new RegrasDaInscricao(PROVA, HOJE_FIXO);

    // ---------- idade e categoria nas bordas ----------

    /**
     * Cada linha é um caso: nascimento, idade esperada na prova e categoria esperada.
     * Para cada borda há o "dia antes do aniversário" (ainda não fez) e o "dia do aniversário".
     */
    @ParameterizedTest(name = "nascido em {0}: {1} anos, {2}")
    @CsvSource({
            // véspera / no dia / dia seguinte da prova
            "2000-03-15, 26, ADULTO",
            "2000-03-14, 27, ADULTO",
            "2000-03-13, 27, ADULTO",
            // borda 13/14 (idade mínima) - quem faz 14 no dia da prova é Juvenil
            "2013-03-14, 14, JUVENIL",
            // borda 17/18
            "2009-03-15, 17, JUVENIL",
            "2009-03-14, 18, ADULTO",
            // borda 39/40 (Valdir pediu pelo ano; pelo regulamento 1987-03-15 ainda é Adulto)
            "1987-03-15, 39, ADULTO",
            "1987-03-14, 40, MASTER",
            // borda 59/60
            "1967-03-15, 59, MASTER",
            "1967-03-14, 60, SENIOR",
    })
    void calculaIdadeNaDataDaProvaECategoria(LocalDate nascimento, int idadeEsperada, Categoria categoriaEsperada) {
        DadosValidos dados = regras.validar("Ana Souza", "ana@email.com", nascimento.toString(), "KM_5");

        int idade = regras.idadeNaProva(dados.dataNascimento());

        assertEquals(idadeEsperada, idade);
        assertEquals(categoriaEsperada, RegrasDaInscricao.categoriaPorIdade(idade));
    }

    @Test
    void quemNasceuEm29DeFevereiroFazAniversarioEm1DeMarco() {
        LocalDate nascimento = LocalDate.of(2008, 2, 29);

        assertEquals(18, new RegrasDaInscricao(LocalDate.of(2027, 2, 28), HOJE_FIXO).idadeNaProva(nascimento));
        assertEquals(19, new RegrasDaInscricao(LocalDate.of(2027, 3, 1), HOJE_FIXO).idadeNaProva(nascimento));
    }

    // ---------- idade mínima e 10 km ----------

    @Test
    void recusaQuemTem13AnosNaProva() {
        assertRecusa("A idade mínima para participar é 14 anos.", "2013-03-15", "KM_5");
    }

    @Test
    void idadeMinimaTemPrecedenciaSobreO10km() {
        // 13 anos pedindo 10 km: a mensagem tem que ser a da idade mínima.
        assertRecusa("A idade mínima para participar é 14 anos.", "2013-03-15", "KM_10");
    }

    @Test
    void recusa10kmParaQuemTem17AnosNaProva() {
        assertRecusa("O percurso de 10 km é permitido somente a partir de 18 anos.", "2009-03-15", "KM_10");
    }

    @Test
    void aceita10kmParaQuemFaz18NoDiaDaProva() {
        DadosValidos dados = regras.validar("Ana Souza", "ana@email.com", "2009-03-14", "KM_10");
        assertEquals(Percurso.KM_10, dados.percurso());
    }

    // ---------- normalização ----------

    @Test
    void normalizaEmailENome() {
        DadosValidos dados = regras.validar("  Ana   Maria  D'Ávila-Souza ", " Ana.Souza@Email.COM ", "1990-05-20", "KM_5");

        assertEquals("Ana Maria D'Ávila-Souza", dados.nome());
        assertEquals("ana.souza@email.com", dados.email());
    }

    // ---------- validação dos campos ----------

    @Test
    void nomeDeveTerDe3a100CaracteresDepoisDeLimpar() {
        regras.validar("Ana", "ana@email.com", "1990-05-20", "KM_5");
        regras.validar("A".repeat(100), "ana@email.com", "1990-05-20", "KM_5");

        String mensagem = "O nome deve ter entre 3 e 100 caracteres.";
        assertEquals(mensagem, recusa("  Al  ", "ana@email.com", "1990-05-20", "KM_5"));
        assertEquals(mensagem, recusa("A".repeat(101), "ana@email.com", "1990-05-20", "KM_5"));
    }

    @Test
    void nomeAceitaSoLetrasEspacosApostrofosEHifens() {
        assertEquals("O nome deve conter apenas letras, espaços, apóstrofos e hífens.",
                recusa("Ana 2", "ana@email.com", "1990-05-20", "KM_5"));
    }

    @Test
    void camposAusentesSaoRecusados() {
        assertEquals("O nome é obrigatório.", recusa(null, "ana@email.com", "1990-05-20", "KM_5"));
        assertEquals("O e-mail é obrigatório.", recusa("Ana Souza", "  ", "1990-05-20", "KM_5"));
        assertEquals("A data de nascimento é obrigatória.", recusa("Ana Souza", "ana@email.com", null, "KM_5"));
        assertEquals("O percurso é obrigatório.", recusa("Ana Souza", "ana@email.com", "1990-05-20", ""));
    }

    @Test
    void emailForaDoFormatoERecusado() {
        assertEquals("Informe um e-mail válido.", recusa("Ana Souza", "ana@email", "1990-05-20", "KM_5"));
        assertEquals("Informe um e-mail válido.", recusa("Ana Souza", "ana email.com", "1990-05-20", "KM_5"));
    }

    @Test
    void dataInexistenteOuForaDoFormatoERecusada() {
        String mensagem = "A data de nascimento deve ser uma data existente no formato aaaa-mm-dd.";
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "2001-02-29", "KM_5"));
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "20/05/1990", "KM_5"));
    }

    @Test
    void nascimentoTemQueSerAnteriorAHoje() {
        String mensagem = "A data de nascimento deve ser anterior a hoje.";
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "2026-11-04", "KM_5")); // hoje
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "2030-01-01", "KM_5")); // futuro
    }

    @Test
    void percursoDesconhecidoERecusado() {
        String mensagem = "O percurso deve ser exatamente KM_5 ou KM_10.";
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "1990-05-20", "KM_7"));
        // O contrato fixa os valores exatos: minúsculas não são aceitas.
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "1990-05-20", "km_5"));
    }

    // ---------- entradas atípicas ----------

    @ParameterizedTest(name = "e-mail recusado: {0}")
    @ValueSource(strings = {
            ".ana@email.com",        // começa com ponto
            "ana.@email.com",        // ponto antes do @
            "ana..souza@email.com",  // dois pontos seguidos
            "ana@.email.com",        // domínio começa com ponto
            "ana@email..com",        // dois pontos seguidos no domínio
            "ana@-email.com",        // hífen no começo do rótulo
            "ana@email-.com",        // hífen no fim do rótulo
            "@email.com",            // sem parte local
            "ana@@email.com",        // dois @
            "ana@email.c",           // terminação com 1 letra
            "ana souza@email.com",   // espaço no meio
    })
    void emailForaDoFormatoERecusado(String email) {
        assertEquals("Informe um e-mail válido.", recusa("Ana Souza", email, "1990-05-20", "KM_5"));
    }

    @ParameterizedTest(name = "e-mail aceito: {0}")
    @ValueSource(strings = {
            "ana.souza@email.com",
            "joao+corrida@gmail.com",
            "a_b-c%d@sub-dominio.email.com.br",
            "x@y.io",
    })
    void emailsComumSaoAceitos(String email) {
        assertEquals(email, regras.validar("Ana Souza", email, "1990-05-20", "KM_5").email());
    }

    @Test
    void emailComTamanhoAcimaDoPermitidoERecusado() {
        String mensagem = "Informe um e-mail válido.";
        assertEquals(mensagem, recusa("Ana Souza", "a".repeat(65) + "@email.com", "1990-05-20", "KM_5")); // local > 64
        assertEquals(mensagem, recusa("Ana Souza", "a@" + "b".repeat(250) + ".com", "1990-05-20", "KM_5")); // total > 254
    }

    @Test
    void espacoNaoSeparavelDeTextoCopiadoDaWebEhRemovidoDasPontas() {
        // U+00A0 é o espaço que o trim() do Java não enxerga.
        String nbsp = "\u00A0";
        DadosValidos dados = regras.validar(
                nbsp + "Ana" + nbsp + "Souza" + nbsp,
                nbsp + "Ana@Email.com" + nbsp,
                nbsp + "1990-05-20" + nbsp,
                nbsp + "KM_5" + nbsp);

        assertEquals("Ana Souza", dados.nome());
        assertEquals("ana@email.com", dados.email());
        assertEquals(LocalDate.of(1990, 5, 20), dados.dataNascimento());
        assertEquals(Percurso.KM_5, dados.percurso());
    }

    @Test
    void tabEQuebraDeLinhaNoNomeViramUmEspaco() {
        assertEquals("Ana Souza Lima", regras.validar("Ana\tSouza\n  Lima", "ana@email.com", "1990-05-20", "KM_5").nome());
    }

    @Test
    void emailEmMinusculasNaoDependeDoIdiomaDoComputador() {
        Locale original = Locale.getDefault();
        try {
            // No turco, "I".toLowerCase() sem Locale.ROOT daria "ı" (i sem ponto).
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("info@email.com", RegrasDaInscricao.normalizarEmail("INFO@EMAIL.COM"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void textoNuloEmTodosOsCamposViraRecusaECampoObrigatorio() {
        assertEquals("O nome é obrigatório.", recusa(null, null, null, null));
    }

    // ---------- auxiliares ----------

    /** Valida e devolve a mensagem da recusa (o teste falha se não houver recusa). */
    private String recusa(String nome, String email, String nascimento, String percurso) {
        InscricaoRecusada erro = assertThrows(InscricaoRecusada.class,
                () -> regras.validar(nome, email, nascimento, percurso));
        assertEquals(400, erro.getStatus());
        return erro.getMessage();
    }

    private void assertRecusa(String mensagemEsperada, String nascimento, String percurso) {
        assertEquals(mensagemEsperada, recusa("Ana Souza", "ana@email.com", nascimento, percurso));
    }
}
