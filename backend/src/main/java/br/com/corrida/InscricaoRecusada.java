package br.com.corrida;

/**
 * Exceção lançada sempre que uma operação é recusada por regra de negócio.
 *
 * Ela carrega duas coisas: a mensagem em português que vai para o cliente
 * e o status HTTP do contrato (400, 404 ou 409). Quem transforma isso em
 * resposta JSON é o TratamentoDeErros.
 *
 * Os construtores ficam privados e a criação é feita pelos três métodos estáticos
 * abaixo, um para cada tipo de recusa do contrato. Assim ninguém inventa um status
 * fora do contrato, e o código que lança fica legível:
 *     throw InscricaoRecusada.naoEncontrada();
 *
 * Estende RuntimeException (exceção "não checada"): não obriga cada método a
 * declarar "throws", e o @Transactional desfaz a transação automaticamente.
 */
public class InscricaoRecusada extends RuntimeException {

    static final String MENSAGEM_NAO_ENCONTRADA = "Não existe inscrição ativa para este e-mail.";
    static final String MENSAGEM_EMAIL_EM_USO = "Já existe uma inscrição ativa para este e-mail.";

    private final int status;

    private InscricaoRecusada(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    /** 400: campo ausente, formato inválido ou regra do regulamento não atendida. */
    public static InscricaoRecusada dadosInvalidos(String mensagem) {
        return new InscricaoRecusada(400, mensagem);
    }

    /** 404: não existe inscrição ativa para o e-mail informado. */
    public static InscricaoRecusada naoEncontrada() {
        return new InscricaoRecusada(404, MENSAGEM_NAO_ENCONTRADA);
    }

    /** 409: o e-mail já tem inscrição ativa (só acontece no cadastro). */
    public static InscricaoRecusada emailEmUso() {
        return new InscricaoRecusada(409, MENSAGEM_EMAIL_EM_USO);
    }

    public int getStatus() {
        return status;
    }
}
