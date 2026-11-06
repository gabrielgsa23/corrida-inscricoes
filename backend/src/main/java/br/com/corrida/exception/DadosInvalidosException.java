package br.com.corrida.exception;

/**
 * 400 Bad Request: campo ausente, formato inválido ou regra do regulamento não atendida.
 * Quem lança é o InscricaoValidator, sempre com uma mensagem específica do problema.
 *
 * Estende RuntimeException (exceção "não checada"): não obriga cada método a declarar
 * "throws", e o @Transactional do service desfaz a transação automaticamente.
 */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
