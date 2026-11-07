package br.com.corrida.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.corrida.dto.InscricaoRequest;
import br.com.corrida.enums.Categoria;
import br.com.corrida.enums.Percurso;
import br.com.corrida.exception.DadosInvalidosException;
import br.com.corrida.validator.InscricaoValidator.DadosValidos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class InscricaoValidatorTest {
    private static final LocalDate PROVA = LocalDate.of(2027, 3, 14);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final Clock HOJE_FIXO = Clock.fixed(
            LocalDate.of(2026, 11, 4).atStartOfDay(FUSO).toInstant(), FUSO);

    private final InscricaoValidator validator = new InscricaoValidator(PROVA, HOJE_FIXO);

    @ParameterizedTest(name = "nascido em {0}: {1} anos, {2}")
    @CsvSource({
            "2000-03-15, 26, ADULTO",
            "2000-03-14, 27, ADULTO",
            "2000-03-13, 27, ADULTO",

            "2013-03-14, 14, JUVENIL",

            "2009-03-15, 17, JUVENIL",
            "2009-03-14, 18, ADULTO",

            "1987-03-15, 39, ADULTO",
            "1987-03-14, 40, MASTER",

            "1967-03-15, 59, MASTER",
            "1967-03-14, 60, SENIOR",
    })
    void calculaIdadeNaDataDaProvaECategoria(String nascimento, int idadeEsperada, Categoria categoriaEsperada) {
        DadosValidos dados = validar("Ana Souza", "ana@email.com", nascimento, "KM_5");

        int idade = validator.idadeNaProva(dados.dataNascimento());

        assertEquals(idadeEsperada, idade);
        assertEquals(categoriaEsperada, Categoria.porIdade(idade));
    }

    @Test
    void recusaQuemTem13AnosNaProva() {
        assertEquals("A idade mínima para participar é 14 anos.", recusaNascimento("2013-03-15", "KM_5"));
    }

    @Test
    void idadeMinimaTemPrecedenciaSobreO10km() {
        assertEquals("A idade mínima para participar é 14 anos.", recusaNascimento("2013-03-15", "KM_10"));
    }

    @Test
    void recusa10kmParaQuemTem17AnosNaProva() {
        assertEquals("O percurso de 10 km é permitido somente a partir de 18 anos.",
                recusaNascimento("2009-03-15", "KM_10"));
    }

    @Test
    void aceita10kmParaQuemFaz18NoDiaDaProva() {
        assertEquals(Percurso.KM_10, validar("Ana Souza", "ana@email.com", "2009-03-14", "KM_10").percurso());
    }

    @Test
    void anoBissextoAceita29DeFevereiro() {
        assertEquals(LocalDate.of(2008, 2, 29),
                validar("Ana Souza", "ana@email.com", "2008-02-29", "KM_5").dataNascimento());
    }

    @Test
    void anoNaoBissextoRecusa29DeFevereiroComMensagemPropria() {
        assertEquals("A data 29/02 só existe em ano bissexto, e 2001 não é bissexto.",
                recusaNascimento("2001-02-29", "KM_5"));
    }

    @Test
    void quemNasceuEm29DeFevereiroFazAniversarioEm1DeMarco() {
        LocalDate nascimento = LocalDate.of(2008, 2, 29);

        assertEquals(18, new InscricaoValidator(LocalDate.of(2027, 2, 28), HOJE_FIXO).idadeNaProva(nascimento));
        assertEquals(19, new InscricaoValidator(LocalDate.of(2027, 3, 1), HOJE_FIXO).idadeNaProva(nascimento));
    }

    @Test
    void diaQueNaoExisteNoMesERecusado() {
        assertEquals("O dia da data de nascimento não existe: o mês 04/2001 tem 30 dias.",
                recusaNascimento("2001-04-31", "KM_5"));
        assertEquals("O dia da data de nascimento não existe: o mês 02/2000 tem 29 dias.",
                recusaNascimento("2000-02-30", "KM_5"));
        assertEquals("O dia da data de nascimento não existe: o mês 05/1990 tem 31 dias.",
                recusaNascimento("1990-05-00", "KM_5"));
    }

    @Test
    void mesQueNaoExisteERecusado() {
        assertEquals("O mês da data de nascimento deve estar entre 01 e 12.", recusaNascimento("1990-13-01", "KM_5"));
        assertEquals("O mês da data de nascimento deve estar entre 01 e 12.", recusaNascimento("1990-00-10", "KM_5"));
    }

    @ParameterizedTest(name = "formato recusado: {0}")
    @ValueSource(strings = {"20/05/1990", "1990-5-20", "1990/05/20", "19900520", "abcd-ef-gh"})
    void dataForaDoFormatoERecusada(String nascimento) {
        assertEquals("A data de nascimento deve estar no formato aaaa-mm-dd.", recusaNascimento(nascimento, "KM_5"));
    }

    @Test
    void dataDeHojeOuFuturaERecusada() {
        String mensagem = "A data de nascimento deve ser anterior a hoje.";
        assertEquals(mensagem, recusaNascimento("2026-11-04", "KM_5"));
        assertEquals(mensagem, recusaNascimento("2030-01-01", "KM_5"));
    }

    @Test
    void dataAntesDe1900ERecusada() {
        assertEquals("A data de nascimento deve ser a partir de 1900-01-01.", recusaNascimento("1899-12-31", "KM_5"));
    }

    @Test
    void anoNormalComEspacosNasPontasEAceito() {
        assertEquals(LocalDate.of(1990, 5, 20),
                validar("Ana Souza", "ana@email.com", "  1990-05-20  ", "KM_5").dataNascimento());
    }

    @Test
    void normalizaNomeEEmail() {
        DadosValidos dados = validar("  Ana   Maria  D'Ávila-Souza ", " Ana.Souza@Email.COM ", "1990-05-20", "KM_5");

        assertEquals("Ana Maria D'Ávila-Souza", dados.nome());
        assertEquals("ana.souza@email.com", dados.email());
    }

    @Test
    void nomeDeveTerDe3a100CaracteresDepoisDeLimpar() {
        validar("Ana", "ana@email.com", "1990-05-20", "KM_5");
        validar("A".repeat(100), "ana@email.com", "1990-05-20", "KM_5");

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
    void tabEQuebraDeLinhaNoNomeViramUmEspaco() {
        assertEquals("Ana Souza Lima", validar("Ana\tSouza\n  Lima", "ana@email.com", "1990-05-20", "KM_5").nome());
    }

    @Test
    void camposAusentesSaoRecusados() {
        assertEquals("O nome é obrigatório.", recusa(null, "ana@email.com", "1990-05-20", "KM_5"));
        assertEquals("O e-mail é obrigatório.", recusa("Ana Souza", "  ", "1990-05-20", "KM_5"));
        assertEquals("A data de nascimento é obrigatória.", recusa("Ana Souza", "ana@email.com", null, "KM_5"));
        assertEquals("O percurso é obrigatório.", recusa("Ana Souza", "ana@email.com", "1990-05-20", ""));
    }

    @Test
    void percursoDesconhecidoOuEmMinusculasERecusado() {
        String mensagem = "O percurso deve ser exatamente KM_5 ou KM_10.";
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "1990-05-20", "KM_7"));
        assertEquals(mensagem, recusa("Ana Souza", "ana@email.com", "1990-05-20", "km_5"));
    }

    @ParameterizedTest(name = "e-mail recusado: {0}")
    @ValueSource(strings = {
            "ana@email",
            "ana email.com",
            ".ana@email.com",
            "ana.@email.com",
            "ana..souza@email.com",
            "ana@.email.com",
            "ana@email..com",
            "ana@-email.com",
            "ana@email-.com",
            "@email.com",
            "ana@@email.com",
            "ana@email.c",
            "ana souza@email.com",
    })
    void emailForaDoFormatoERecusado(String email) {
        assertEquals("Informe um e-mail válido.", recusa("Ana Souza", email, "1990-05-20", "KM_5"));
    }

    @ParameterizedTest(name = "e-mail aceito: {0}")
    @ValueSource(strings = {"ana.souza@email.com", "joao+corrida@gmail.com", "a_b-c%d@sub-dominio.email.com.br", "x@y.io"})
    void emailsComunsSaoAceitos(String email) {
        assertEquals(email, validar("Ana Souza", email, "1990-05-20", "KM_5").email());
    }

    @Test
    void emailComTamanhoAcimaDoPermitidoERecusado() {
        String mensagem = "Informe um e-mail válido.";
        assertEquals(mensagem, recusa("Ana Souza", "a".repeat(65) + "@email.com", "1990-05-20", "KM_5"));
        assertEquals(mensagem, recusa("Ana Souza", "a@" + "b".repeat(250) + ".com", "1990-05-20", "KM_5"));
    }

    @Test
    void emailEmMinusculasNaoDependeDoIdiomaDoComputador() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("info@email.com", InscricaoValidator.normalizarEmail("INFO@EMAIL.COM"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void espacoNaoSeparavelEhRemovidoDasPontasDeTodosOsCampos() {
        String nbsp = " ";
        DadosValidos dados = validar(
                nbsp + "Ana" + nbsp + "Souza" + nbsp,
                nbsp + "Ana@Email.com" + nbsp,
                nbsp + "1990-05-20" + nbsp,
                nbsp + "KM_5" + nbsp);

        assertEquals("Ana Souza", dados.nome());
        assertEquals("ana@email.com", dados.email());
        assertEquals(LocalDate.of(1990, 5, 20), dados.dataNascimento());
        assertEquals(Percurso.KM_5, dados.percurso());
    }

    private DadosValidos validar(String nome, String email, String nascimento, String percurso) {
        return validator.validar(new InscricaoRequest(nome, email, nascimento, percurso), email);
    }

    private String recusa(String nome, String email, String nascimento, String percurso) {
        return assertThrows(DadosInvalidosException.class,
                () -> validar(nome, email, nascimento, percurso)).getMessage();
    }

    private String recusaNascimento(String nascimento, String percurso) {
        return recusa("Ana Souza", "ana@email.com", nascimento, percurso);
    }
}
