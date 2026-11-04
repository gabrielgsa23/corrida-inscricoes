package br.com.corrida;

import br.com.corrida.Contrato.Mensagem;
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
 * @RestControllerAdvice = "vale para todos os controllers". Quando um método de controller
 * (ou algo que ele chamou) lança uma exceção, o Spring procura aqui o @ExceptionHandler
 * do tipo mais específico que combina com ela.
 */
@RestControllerAdvice
public class TratamentoDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratamentoDeErros.class);

    /** Nossas recusas de negócio: 400, 404 ou 409, com a mensagem que a regra escolheu. */
    @ExceptionHandler(InscricaoRecusada.class)
    public ResponseEntity<Mensagem> recusa(InscricaoRecusada erro) {
        return ResponseEntity.status(erro.getStatus()).body(new Mensagem(erro.getMessage()));
    }

    /**
     * O Jackson não conseguiu ler o corpo: JSON quebrado ({"nome": ), corpo vazio,
     * ou um tipo impossível de converter (ex.: "nome": [1, 2]). Sem este handler
     * o cliente receberia um erro genérico do Spring.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Mensagem> corpoIlegivel() {
        return ResponseEntity.badRequest()
                .body(new Mensagem("O corpo da requisição está vazio ou não é um JSON válido."));
    }

    /**
     * Rede de segurança para dois cadastros do mesmo e-mail chegando ao mesmo tempo:
     * os dois passam pelo existsByEmail, mas a restrição unique do banco barra o segundo.
     * Em vez de 500, devolvemos o mesmo 409 do caminho normal.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Mensagem> violacaoNoBanco() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new Mensagem(InscricaoRecusada.MENSAGEM_EMAIL_EM_USO));
    }

    /**
     * Todo o resto.
     *
     * Exceções do próprio Spring MVC (rota que não existe, método errado, Content-Type
     * errado) implementam a interface ErrorResponse e já sabem qual é o status certo
     * (404, 405, 415...). Nesses casos só trocamos a mensagem por uma em português.
     *
     * Qualquer outra coisa é bug nosso: registramos o erro completo no console (para o
     * desenvolvedor investigar) e o cliente recebe só uma mensagem genérica com 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Mensagem> inesperado(Exception erro) {
        if (erro instanceof ErrorResponse erroDoSpring) {
            HttpStatusCode status = erroDoSpring.getStatusCode();
            return ResponseEntity.status(status)
                    .headers(erroDoSpring.getHeaders()) // ex.: o 405 informa quais métodos são aceitos
                    .body(new Mensagem(mensagemPorStatus(status.value())));
        }

        log.error("Erro inesperado ao processar a requisição", erro);
        return ResponseEntity.internalServerError()
                .body(new Mensagem("Erro interno. Tente novamente em instantes."));
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
