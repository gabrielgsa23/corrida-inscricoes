package br.com.corrida.validator;

import br.com.corrida.dto.InscricaoRequest;
import br.com.corrida.enums.Percurso;
import br.com.corrida.exception.DadosInvalidosException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Todas as regras de entrada da inscrição ficam aqui:
 * limpeza dos dados (normalização), validação de cada campo e o regulamento da prova
 * (idade mínima e restrição dos 10 km).
 *
 * É Java puro, sem anotação do Spring. Isso tem duas vantagens:
 * 1) os testes criam a classe com "new" e rodam em milissegundos, sem subir servidor;
 * 2) o cadastro e a edição chamam o mesmo validar(), então as regras nunca ficam
 *    diferentes entre um e outro (sem código repetido).
 *
 * Quem cria o objeto é o CorridaApplication, com a data da prova do application.properties.
 */
public class InscricaoValidator {

    /**
     * Resultado de uma validação bem-sucedida: os campos já limpos e convertidos para o
     * tipo certo (o texto da data virou LocalDate, o texto do percurso virou enum).
     */
    public record DadosValidos(String nome, String email, LocalDate dataNascimento, Percurso percurso) {
    }

    // Limites do regulamento (Art. 6º, II e III). Em constantes para que uma alteração
    // ao vivo ("idade mínima agora é 16") seja uma linha só.
    static final int IDADE_MINIMA = 14;
    static final int IDADE_MINIMA_10KM = 18;

    // Limites do nome definidos pelo enunciado.
    static final int NOME_MIN = 3;
    static final int NOME_MAX = 100;

    // Premissa: o enunciado não define limite inferior, mas sem ele
    // "0001-01-01" seria aceito como alguém de 2026 anos.
    static final LocalDate NASCIMENTO_MAIS_ANTIGO = LocalDate.of(1900, 1, 1);

    // Tamanhos máximos de um e-mail real (RFC 5321): 64 antes do @ e 254 no total.
    // 254 também é o tamanho da coluna no banco.
    static final int EMAIL_LOCAL_MAX = 64;
    static final int EMAIL_MAX = 254;

    // Nome: "palavras" de letras separadas por UM espaço, apóstrofo ou hífen.
    //   \p{L} = qualquer letra de qualquer idioma (á, ç, ñ...)
    //   \p{M} = acentos "soltos" que alguns teclados mandam separados da letra
    //   ’     = apóstrofo tipográfico, comum em texto vindo do Word/celular
    // Aceita: "Ana Souza", "D'Ávila", "Jean-Luc". Recusa: "Ana2", "---", "Ana -".
    private static final Pattern NOME_VALIDO =
            Pattern.compile("[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*");

    // E-mail (já em minúsculas) no formato parte-local@dominio:
    //   parte local: blocos de letras/números/_%+- separados por UM ponto
    //                (".ana@x.com", "ana.@x.com" e "ana..souza@x.com" são recusados);
    //   domínio: rótulos sem hífen nas pontas, terminando em 2+ letras
    //                ("ana@email", "ana@x..com" e "ana@-x.com" são recusados).
    // Simples de propósito: barra erro de digitação, não implementa a RFC inteira.
    private static final Pattern EMAIL_VALIDO = Pattern.compile(
            "[a-z0-9_%+-]+(?:\\.[a-z0-9_%+-]+)*"
                    + "@[a-z0-9]+(?:-[a-z0-9]+)*(?:\\.[a-z0-9]+(?:-[a-z0-9]+)*)*\\.[a-z]{2,}");

    // Data: exatamente 4 dígitos, hífen, 2 dígitos, hífen, 2 dígitos (aaaa-mm-dd).
    private static final Pattern FORMATO_DATA = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    private final LocalDate dataProva;

    // Clock em vez de LocalDate.now() direto: assim o teste consegue "congelar" o dia de hoje.
    private final Clock relogio;

    public InscricaoValidator(LocalDate dataProva, Clock relogio) {
        this.dataProva = dataProva;
        this.relogio = relogio;
    }

    // ------------------------------------------------------------------
    // Validação completa (usada no cadastro E na edição)
    // ------------------------------------------------------------------

    /**
     * Normaliza e valida os campos e depois aplica o regulamento. Devolve os dados prontos
     * ou lança DadosInvalidosException (vira 400) na primeira regra que falhar.
     *
     * O e-mail vem num parâmetro separado porque na edição ele não sai do corpo:
     * o service passa o e-mail já gravado, e o do corpo é ignorado.
     *
     * Ordem: nome, e-mail, nascimento, percurso e só então o regulamento.
     */
    public DadosValidos validar(InscricaoRequest dados, String email) {
        String nome = validarNome(dados.nome());
        String emailLimpo = validarEmail(email);
        LocalDate nascimento = validarNascimento(dados.dataNascimento());
        Percurso percurso = validarPercurso(dados.percurso());

        verificarRegulamento(nascimento, percurso);

        return new DadosValidos(nome, emailLimpo, nascimento, percurso);
    }

    /**
     * Idade COMPLETA na data da prova (Art. 6º, I), e não a idade de hoje nem a
     * diferença entre os anos. Period.between conta anos, meses e dias; getYears()
     * pega só os anos completos. Com prova em 14/03/2027:
     *   nascido em 15/03/2000 -> 26 (faz 27 só no dia seguinte à prova)
     *   nascido em 14/03/2000 -> 27 (aniversário no dia da prova já conta)
     * Quem nasceu em 29/02 completa ano em 01/03 nos anos não bissextos.
     */
    public int idadeNaProva(LocalDate nascimento) {
        return Period.between(nascimento, dataProva).getYears();
    }

    /**
     * " Ana.Souza@Email.COM " vira "ana.souza@email.com". É assim que o e-mail vira chave.
     * Pública porque o service também usa para buscar pelo e-mail da URL.
     * Locale.ROOT deixa a conversão igual em qualquer computador: sem ele, num Windows
     * em turco, o "I" maiúsculo viraria "ı" (i sem ponto).
     */
    public static String normalizarEmail(String email) {
        return aparar(email).toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // Regulamento
    // ------------------------------------------------------------------

    /**
     * A idade mínima é conferida ANTES do percurso (pedido do enunciado):
     * alguém de 13 anos pedindo 10 km recebe "idade mínima", não "10 km não permitido".
     */
    private void verificarRegulamento(LocalDate nascimento, Percurso percurso) {
        int idade = idadeNaProva(nascimento);

        if (idade < IDADE_MINIMA) {
            throw new DadosInvalidosException(
                    "A idade mínima para participar é " + IDADE_MINIMA + " anos.");
        }
        if (percurso == Percurso.KM_10 && idade < IDADE_MINIMA_10KM) {
            throw new DadosInvalidosException(
                    "O percurso de 10 km é permitido somente a partir de " + IDADE_MINIMA_10KM + " anos.");
        }
    }

    // ------------------------------------------------------------------
    // Validação de cada campo
    // ------------------------------------------------------------------

    private String validarNome(String nome) {
        // Tira espaços das pontas e troca qualquer sequência interna (tab, quebra de linha...)
        // por um espaço só. O (?U) faz o \s reconhecer também os espaços Unicode.
        String limpo = aparar(nome).replaceAll("(?U)\\s+", " ");
        if (limpo.isEmpty()) {
            throw new DadosInvalidosException("O nome é obrigatório.");
        }
        // O tamanho é medido DEPOIS da limpeza, como pede o enunciado.
        if (limpo.length() < NOME_MIN || limpo.length() > NOME_MAX) {
            throw new DadosInvalidosException(
                    "O nome deve ter entre " + NOME_MIN + " e " + NOME_MAX + " caracteres.");
        }
        if (!NOME_VALIDO.matcher(limpo).matches()) {
            throw new DadosInvalidosException("O nome deve conter apenas letras, espaços, apóstrofos e hífens.");
        }
        return limpo;
    }

    private String validarEmail(String email) {
        String limpo = normalizarEmail(email);
        if (limpo.isEmpty()) {
            throw new DadosInvalidosException("O e-mail é obrigatório.");
        }
        // Os tamanhos vêm ANTES da regex, para um texto gigante nem chegar a ela.
        if (limpo.length() > EMAIL_MAX
                || limpo.indexOf('@') > EMAIL_LOCAL_MAX
                || !EMAIL_VALIDO.matcher(limpo).matches()) {
            throw new DadosInvalidosException("Informe um e-mail válido.");
        }
        return limpo;
    }

    /**
     * Confere a data em etapas, cada uma com a sua mensagem:
     *   " 1990-05-20 " -> aceita (os espaços das pontas são removidos)
     *   "20/05/1990"   -> formato errado
     *   "1990-13-01"   -> mês inexistente
     *   "2001-02-29"   -> 29/02 em ano que não é bissexto
     *   "2001-04-31"   -> dia que não existe naquele mês
     *   "2030-01-01"   -> data futura
     */
    private LocalDate validarNascimento(String dataNascimento) {
        String texto = aparar(dataNascimento);
        if (texto.isEmpty()) {
            throw new DadosInvalidosException("A data de nascimento é obrigatória.");
        }
        if (!FORMATO_DATA.matcher(texto).matches()) {
            throw new DadosInvalidosException("A data de nascimento deve estar no formato aaaa-mm-dd.");
        }

        int ano = Integer.parseInt(texto.substring(0, 4));
        int mes = Integer.parseInt(texto.substring(5, 7));
        int dia = Integer.parseInt(texto.substring(8, 10));

        if (mes < 1 || mes > 12) {
            throw new DadosInvalidosException("O mês da data de nascimento deve estar entre 01 e 12.");
        }

        // YearMonth sabe quantos dias tem cada mês, inclusive fevereiro em ano bissexto (29).
        int diasNoMes = YearMonth.of(ano, mes).lengthOfMonth();
        if (dia < 1 || dia > diasNoMes) {
            if (mes == 2 && dia == 29) {
                throw new DadosInvalidosException(
                        "A data 29/02 só existe em ano bissexto, e " + ano + " não é bissexto.");
            }
            throw new DadosInvalidosException(
                    "O dia da data de nascimento não existe: o mês " + texto.substring(5, 7)
                            + "/" + ano + " tem " + diasNoMes + " dias.");
        }

        // LocalDate não tem hora nem fuso, então a data nunca "volta um dia".
        LocalDate data = LocalDate.of(ano, mes, dia);

        if (!data.isBefore(LocalDate.now(relogio))) {
            throw new DadosInvalidosException("A data de nascimento deve ser anterior a hoje.");
        }
        if (data.isBefore(NASCIMENTO_MAIS_ANTIGO)) {
            throw new DadosInvalidosException("A data de nascimento deve ser a partir de 1900-01-01.");
        }
        return data;
    }

    private Percurso validarPercurso(String percurso) {
        String texto = aparar(percurso);
        if (texto.isEmpty()) {
            throw new DadosInvalidosException("O percurso é obrigatório.");
        }
        try {
            // valueOf("KM_5") devolve Percurso.KM_5; qualquer outro texto lança exceção,
            // inclusive "km_5": o contrato fixa os valores exatos KM_5 e KM_10.
            return Percurso.valueOf(texto);
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException("O percurso deve ser exatamente KM_5 ou KM_10.");
        }
    }

    /**
     * Remove os espaços das pontas, inclusive o espaço "não separável" (U+00A0) que vem
     * junto quando o texto é copiado da web, de PDFs ou do Word. O trim() do Java só
     * conhece o espaço comum; o (?U) ensina o \s a reconhecer os espaços Unicode.
     * Texto nulo vira "", então o resto do código não precisa checar null.
     */
    private static String aparar(String texto) {
        return texto == null ? "" : texto.replaceAll("(?U)^\\s+|\\s+$", "");
    }
}
