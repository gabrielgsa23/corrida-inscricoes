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

public class InscricaoValidator {
    public record DadosValidos(String nome, String email, LocalDate dataNascimento, Percurso percurso) {
    }

    static final int IDADE_MINIMA = 14;
    static final int IDADE_MINIMA_10KM = 18;
    static final int NOME_MIN = 3;
    static final int NOME_MAX = 100;
    static final int EMAIL_LOCAL_MAX = 64;
    static final int EMAIL_MAX = 254;
    static final LocalDate NASCIMENTO_MAIS_ANTIGO = LocalDate.of(1900, 1, 1);

    private static final Pattern NOME_VALIDO =
            Pattern.compile("[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*");

    private static final Pattern EMAIL_VALIDO = Pattern.compile(
            "[a-z0-9_%+-]+(?:\\.[a-z0-9_%+-]+)*"
                    + "@[a-z0-9]+(?:-[a-z0-9]+)*(?:\\.[a-z0-9]+(?:-[a-z0-9]+)*)*\\.[a-z]{2,}");

    private static final Pattern FORMATO_DATA = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    private final LocalDate dataProva;
    private final Clock relogio;

    public InscricaoValidator(LocalDate dataProva, Clock relogio) {
        this.dataProva = dataProva;
        this.relogio = relogio;
    }

    public DadosValidos validar(InscricaoRequest dados, String email) {
        String nome = validarNome(dados.nome());
        String emailLimpo = validarEmail(email);
        LocalDate nascimento = validarNascimento(dados.dataNascimento());
        Percurso percurso = validarPercurso(dados.percurso());

        verificarRegulamento(nascimento, percurso);

        return new DadosValidos(nome, emailLimpo, nascimento, percurso);
    }

    public int idadeNaProva(LocalDate nascimento) {
        return Period.between(nascimento, dataProva).getYears();
    }

    public static String normalizarEmail(String email) {
        return aparar(email).toLowerCase(Locale.ROOT);
    }

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

    private String validarNome(String nome) {
        String limpo = aparar(nome).replaceAll("(?U)\\s+", " ");
        if (limpo.isEmpty()) {
            throw new DadosInvalidosException("O nome é obrigatório.");
        }

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

        if (limpo.length() > EMAIL_MAX
                || limpo.indexOf('@') > EMAIL_LOCAL_MAX
                || !EMAIL_VALIDO.matcher(limpo).matches()) {
            throw new DadosInvalidosException("Informe um e-mail válido.");
        }
        return limpo;
    }

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
            return Percurso.valueOf(texto);
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException("O percurso deve ser exatamente KM_5 ou KM_10.");
        }
    }

    private static String aparar(String texto) {
        return texto == null ? "" : texto.replaceAll("(?U)^\\s+|\\s+$", "");
    }
}
