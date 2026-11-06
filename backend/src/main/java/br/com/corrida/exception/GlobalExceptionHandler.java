package br.com.corrida.exception;

import br.com.corrida.dto.ErroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Transforma QUALQUER exceção em uma resposta { "mensagem": "..." }, como pede o contrato.
 * O cliente nunca recebe stack trace nem mensagem interna do Spring.
 *
 * @RestControllerAdvice = "vale para todos os controllers". Quando algo lança uma exceção,
 * o Spring procura aqui o @ExceptionHandler do tipo mais específico que combina com ela.
 * Cada exceção nossa tem o seu método, e o método decide o status HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(DadosInvalidosException erro) {
        return responder(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(InscricaoNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(InscricaoNaoEncontradaException erro) {
        return responder(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponse> emailJaCadastrado(EmailJaCadastradoException erro) {
        return responder(HttpStatus.CONFLICT, erro.getMessage());
    }

    /**
     * Rede de segurança para dois cadastros do mesmo e-mail chegando ao mesmo tempo:
     * os dois passam pelo existsByEmail, mas a restrição unique do banco barra o segundo.
     * Em vez de 500, devolvemos o mesmo 409 do caminho normal (reaproveitando a mensagem).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> violacaoNoBanco() {
        return emailJaCadastrado(new EmailJaCadastradoException());
    }

    /**
     * O Jackson não conseguiu ler o corpo: JSON quebrado ({"nome": ), corpo vazio,
     * ou um tipo impossível de converter (ex.: "nome": [1, 2]).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel() {
        return responder(HttpStatus.BAD_REQUEST, "O corpo da requisição está vazio ou não é um JSON válido.");
    }

    /**
     * Todo o resto.
     *
     * Exceções do próprio Spring MVC (rota que não existe, método errado, Content-Type
     * errado) implementam a interface ErrorResponse e já sabem o status certo
     * (404, 405, 415...). Nesses casos só trocamos a mensagem por uma em português.
     *
     * Qualquer outra coisa é bug nosso: registramos o erro completo no console (para o
     * desenvolvedor investigar) e o cliente recebe só uma mensagem genérica com 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception erro) {
        if (erro instanceof ErrorResponse erroDoSpring) {
            HttpStatusCode status = erroDoSpring.getStatusCode();
            return ResponseEntity.status(status)
                    .headers(erroDoSpring.getHeaders()) // ex.: o 405 informa quais métodos são aceitos
                    .body(new ErroResponse(mensagemPorStatus(status.value())));
        }

        log.error("Erro inesperado ao processar a requisição", erro);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.");
    }

    // ------------------------------------------------------------------

    /** Monta a resposta padrão de erro. Todos os handlers passam por aqui. */
    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(mensagem));
    }

    private String mensagemPorStatus(int status) {
        return switch (status) {
            case 404 -> "Rota não encontrada.";
            case 405 -> "Método HTTP não permitido nesta rota.";
            case 415 -> "Envie o corpo em JSON, com o cabeçalho Content-Type: application/json.";
            default -> "Requisição inválida.";
        };
    }
}
