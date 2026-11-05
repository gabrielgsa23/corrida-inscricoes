package br.com.corrida;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Todas as regras de negócio da inscrição ficam neste arquivo:
 * limpeza dos dados (normalização), validação dos campos e o regulamento da prova
 * (idade mínima, restrição dos 10 km e categorias).
 *
 * Esta classe é Java puro, sem nenhuma anotação do Spring. Isso tem duas vantagens:
 * 1) os testes criam a classe com "new" e rodam em milissegundos, sem subir servidor;
 * 2) o cadastro e a edição chamam o mesmo método validar(), então as regras
 *    nunca ficam diferentes entre um e outro (DRY).
 *
 * Quem cria o objeto é o CorridaApplication, passando a data da prova
 * lida do application.properties.
 */
public class RegrasDaInscricao {

    /** Os dois percursos aceitos. O nome da constante é exatamente o texto do JSON. */
    public enum Percurso { KM_5, KM_10 }

    /** Categorias do Art. 6º, IV do regulamento. */
    public enum Categoria { JUVENIL, ADULTO, MASTER, SENIOR }

    /**
     * Resultado de uma validação bem-sucedida: os quatro campos já limpos e convertidos
     * para o tipo certo (texto da data virou LocalDate, texto do percurso virou enum).
     * "record" é uma classe imutável que o Java escreve pra gente (construtor, getters, equals).
     */
    public record DadosValidos(String nome, String email, LocalDate dataNascimento, Percurso percurso) {}

    // Limites do regulamento (Art. 6º, II e III). Ficam em constantes para
    // uma alteração ao vivo ("idade mínima agora é 16") ser uma linha só.
    static final int IDADE_MINIMA = 14;
    static final int IDADE_MINIMA_10KM = 18;

    // Limites do nome definidos pelo enunciado.
    static final int NOME_MIN = 3;
    static final int NOME_MAX = 100;

    // Premissa minha: o enunciado não define um limite inferior, mas sem ele
    // "0001-01-01" seria aceito como alguém de 2026 anos.
    static final LocalDate NASCIMENTO_MAIS_ANTIGO = LocalDate.of(1900, 1, 1);

    // Nome: uma ou mais "palavras" de letras, separadas por UM espaço, apóstrofo ou hífen.
    //   \p{L} = qualquer letra de qualquer idioma (inclui á, ç, ñ...)
    //   \p{M} = acentos "soltos" que alguns teclados mandam separados da letra
    //   ’ = apóstrofo tipográfico (’), comum quando o texto vem do Word/celular
    // Aceita: "Ana Souza", "D'Ávila", "Jean-Luc". Recusa: "Ana2", "---", "Ana -".
    private static final Pattern NOME_VALIDO =
            Pattern.compile("[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*");

    // E-mail (já em minúsculas), no formato parte-local@dominio.
    //   parte local: blocos de letras/números/_%+- separados por UM ponto, então
    //                ".ana@x.com", "ana.@x.com" e "ana..souza@x.com" são recusados;
    //   domínio: rótulos sem hífen nas pontas, separados por ponto, terminando em
    //            2+ letras ("ana@email", "ana@x..com" e "ana@-x.com" são recusados).
    // Simples de propósito: o objetivo é barrar erro de digitação, não a RFC inteira.
    private static final Pattern EMAIL_VALIDO = Pattern.compile(
            "[a-z0-9_%+-]+(?:\\.[a-z0-9_%+-]+)*"
                    + "@[a-z0-9]+(?:-[a-z0-9]+)*(?:\\.[a-z0-9]+(?:-[a-z0-9]+)*)*\\.[a-z]{2,}");

    // Tamanhos máximos de um e-mail real (RFC 5321): 64 antes do @ e 254 no total.
    // 254 também é o tamanho da coluna no banco.
    private static final int EMAIL_LOCAL_MAX = 64;
    private static final int EMAIL_MAX = 254;

    private final LocalDate dataProva;

    // Clock em vez de LocalDate.now() direto: assim o teste consegue "congelar" o dia de hoje.
    private final Clock relogio;

    public RegrasDaInscricao(LocalDate dataProva, Clock relogio) {
        this.dataProva = dataProva;
        this.relogio = relogio;
    }

    // ------------------------------------------------------------------
    // Validação completa (usada no cadastro E na edição)
    // ------------------------------------------------------------------

    /**
     * Normaliza e valida os quatro campos e depois aplica o regulamento.
     * Recebe tudo como String (do jeito que veio no JSON) e devolve os dados prontos,
     * ou lança InscricaoRecusada (vira 400) na primeira regra que falhar.
     *
     * A ordem é: campos (nome, e-mail, nascimento, percurso) e só então o regulamento.
     * Dentro do regulamento, a idade mínima vem antes da regra dos 10 km.
     */
    public DadosValidos validar(String nome, String email, String dataNascimento, String percurso) {
        String nomeLimpo = validarNome(nome);
        String emailLimpo = validarEmail(email);
        LocalDate nascimento = validarNascimento(dataNascimento);
        Percurso percursoEscolhido = validarPercurso(percurso);

        verificarRegulamento(nascimento, percursoEscolhido);

        return new DadosValidos(nomeLimpo, emailLimpo, nascimento, percursoEscolhido);
    }

    // ------------------------------------------------------------------
    // Normalização (pública porque o service também usa para buscar pelo e-mail da URL)
    // ------------------------------------------------------------------

    /**
     * " Ana.Souza@Email.COM " vira "ana.souza@email.com". É assim que o e-mail vira chave.
     * Locale.ROOT faz a conversão para minúsculas ser igual em qualquer computador: sem ele,
     * num Windows configurado em turco, o "I" maiúsculo viraria "ı" (i sem ponto).
     */
    public static String normalizarEmail(String email) {
        return aparar(email).toLowerCase(Locale.ROOT);
    }

    /** Tira espaços das pontas e troca qualquer sequência de espaços internos por um só. */
    static String normalizarNome(String nome) {
        return aparar(nome).replaceAll("(?U)\\s+", " ");
    }

    /**
     * Remove os espaços das pontas, inclusive o espaço "não separável" (U+00A0) que vem
     * junto quando o texto é copiado de páginas web, PDFs ou do Word. O trim() do Java
     * só conhece o espaço comum, e o (?U) ensina o \s a reconhecer os espaços Unicode.
     * Texto nulo vira "", então o resto do código não precisa checar null.
     */
    private static String aparar(String texto) {
        return texto == null ? "" : texto.replaceAll("(?U)^\\s+|\\s+$", "");
    }

    // ------------------------------------------------------------------
    // Regulamento: idade e categoria
    // ------------------------------------------------------------------

    /**
     * Idade COMPLETA na data da prova (Art. 6º, I), e não a idade de hoje
     * nem a simples diferença entre os anos.
     *
     * Period.between conta anos, meses e dias entre as duas datas; getYears() pega só
     * os anos completos. Exemplo com prova em 14/03/2027:
     *   nascido em 15/03/2000 -> 26 (faz 27 só no dia seguinte à prova)
     *   nascido em 14/03/2000 -> 27 (aniversário no próprio dia da prova já conta)
     * Quem nasceu em 29/02 completa ano em 01/03 nos anos não bissextos.
     */
    public int idadeNaProva(LocalDate nascimento) {
        return Period.between(nascimento, dataProva).getYears();
    }

    /** Art. 6º, IV: Juvenil 14–17, Adulto 18–39, Master 40–59, Sênior 60+. */
    public static Categoria categoriaPorIdade(int idade) {
        if (idade < 18) {
            return Categoria.JUVENIL;
        }
        if (idade < 40) {
            return Categoria.ADULTO;
        }
        if (idade < 60) {
            return Categoria.MASTER;
        }
        return Categoria.SENIOR;
    }

    /**
     * A idade mínima é conferida ANTES do percurso (pedido do enunciado):
     * um menino de 13 anos pedindo 10 km recebe "idade mínima", não "10 km não permitido".
     */
    private void verificarRegulamento(LocalDate nascimento, Percurso percurso) {
        int idade = idadeNaProva(nascimento);

        if (idade < IDADE_MINIMA) {
            throw InscricaoRecusada.dadosInvalidos(
                    "A idade mínima para participar é " + IDADE_MINIMA + " anos.");
        }
        if (percurso == Percurso.KM_10 && idade < IDADE_MINIMA_10KM) {
            throw InscricaoRecusada.dadosInvalidos(
                    "O percurso de 10 km é permitido somente a partir de " + IDADE_MINIMA_10KM + " anos.");
        }
    }

    // ------------------------------------------------------------------
    // Validação de cada campo
    // ------------------------------------------------------------------

    private String validarNome(String nome) {
        String limpo = normalizarNome(nome);
        if (limpo.isEmpty()) {
            throw InscricaoRecusada.dadosInvalidos("O nome é obrigatório.");
        }
        // O tamanho é medido DEPOIS da limpeza, como pede o enunciado.
        if (limpo.length() < NOME_MIN || limpo.length() > NOME_MAX) {
            throw InscricaoRecusada.dadosInvalidos(
                    "O nome deve ter entre " + NOME_MIN + " e " + NOME_MAX + " caracteres.");
        }
        if (!NOME_VALIDO.matcher(limpo).matches()) {
            throw InscricaoRecusada.dadosInvalidos(
                    "O nome deve conter apenas letras, espaços, apóstrofos e hífens.");
        }
        return limpo;
    }

    private String validarEmail(String email) {
        String limpo = normalizarEmail(email);
        if (limpo.isEmpty()) {
            throw InscricaoRecusada.dadosInvalidos("O e-mail é obrigatório.");
        }
        // Os tamanhos são conferidos ANTES da regex, para um texto gigante nem chegar a ela.
        if (limpo.length() > EMAIL_MAX
                || limpo.indexOf('@') > EMAIL_LOCAL_MAX
                || !EMAIL_VALIDO.matcher(limpo).matches()) {
            throw InscricaoRecusada.dadosInvalidos("Informe um e-mail válido.");
        }
        return limpo;
    }

    private LocalDate validarNascimento(String dataNascimento) {
        String texto = aparar(dataNascimento);
        if (texto.isEmpty()) {
            throw InscricaoRecusada.dadosInvalidos("A data de nascimento é obrigatória.");
        }

        // LocalDate.parse usa o formato ISO (aaaa-mm-dd) no modo ESTRITO:
        // "2023-02-30" e "2023-2-3" lançam DateTimeParseException.
        // LocalDate não tem hora nem fuso, então a data nunca "volta um dia".
        LocalDate data;
        try {
            data = LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw InscricaoRecusada.dadosInvalidos(
                    "A data de nascimento deve ser uma data existente no formato aaaa-mm-dd.");
        }

        LocalDate hoje = LocalDate.now(relogio);
        if (!data.isBefore(hoje)) {
            throw InscricaoRecusada.dadosInvalidos("A data de nascimento deve ser anterior a hoje.");
        }
        if (data.isBefore(NASCIMENTO_MAIS_ANTIGO)) {
            throw InscricaoRecusada.dadosInvalidos("A data de nascimento deve ser a partir de 1900-01-01.");
        }
        return data;
    }

    private Percurso validarPercurso(String percurso) {
        String texto = aparar(percurso);
        if (texto.isEmpty()) {
            throw InscricaoRecusada.dadosInvalidos("O percurso é obrigatório.");
        }
        try {
            // valueOf("KM_5") devolve Percurso.KM_5; qualquer outro texto lança exceção,
            // inclusive "km_5": o contrato fixa os valores exatos KM_5 e KM_10.
            return Percurso.valueOf(texto);
        } catch (IllegalArgumentException e) {
            throw InscricaoRecusada.dadosInvalidos("O percurso deve ser exatamente KM_5 ou KM_10.");
        }
    }
}
