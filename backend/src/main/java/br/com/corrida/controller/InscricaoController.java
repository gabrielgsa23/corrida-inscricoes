package br.com.corrida.controller;

import br.com.corrida.dto.InscricaoRequest;
import br.com.corrida.dto.InscricaoResponse;
import br.com.corrida.dto.ResumoResponse;
import br.com.corrida.service.InscricaoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta de entrada HTTP. Cada método é uma linha da tabela "Contrato da API".
 *
 * O controller só traduz HTTP <-> Java: lê a URL e o corpo, chama o service e
 * escolhe o status de sucesso. Nenhuma regra de negócio fica aqui (pedido do enunciado).
 * As recusas (400/404/409) são exceções que o GlobalExceptionHandler transforma em JSON.
 *
 * @RestController = os retornos dos métodos viram JSON automaticamente.
 * @RequestMapping  = prefixo comum de todas as rotas desta classe.
 */
@RestController
@RequestMapping("/api/inscricoes")
public class InscricaoController {

    private final InscricaoService service;

    // Injeção de dependência pelo construtor: o Spring cria o service e passa aqui.
    public InscricaoController(InscricaoService service) {
        this.service = service;
    }

    /** POST /api/inscricoes -> 201 com a inscrição criada. @RequestBody lê o JSON do corpo. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse cadastrar(@RequestBody InscricaoRequest dados) {
        return service.cadastrar(dados);
    }

    /** GET /api/inscricoes -> 200 com a lista em ordem de cadastro ([] se vazia). */
    @GetMapping
    public List<InscricaoResponse> listar() {
        return service.listar();
    }

    /**
     * GET /api/inscricoes/resumo -> 200 com total e quantidade por percurso.
     * Não conflita com /{email}: o Spring dá prioridade à rota com texto fixo.
     */
    @GetMapping("/resumo")
    public ResumoResponse resumo() {
        return service.resumo();
    }

    /** GET /api/inscricoes/{email} -> 200 com a inscrição. @PathVariable pega o {email} da URL. */
    @GetMapping("/{email}")
    public InscricaoResponse consultar(@PathVariable String email) {
        return service.consultar(email);
    }

    /** PUT /api/inscricoes/{email} -> 200 com a inscrição atualizada. */
    @PutMapping("/{email}")
    public InscricaoResponse editar(@PathVariable String email, @RequestBody InscricaoRequest dados) {
        return service.editar(email, dados);
    }

    /** DELETE /api/inscricoes/{email} -> 204, sem corpo. */
    @DeleteMapping("/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable String email) {
        service.cancelar(email);
    }
}
