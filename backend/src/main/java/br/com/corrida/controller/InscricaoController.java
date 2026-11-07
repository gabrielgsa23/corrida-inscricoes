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

@RestController
@RequestMapping("/api/inscricoes")
public class InscricaoController {
    private final InscricaoService service;

    public InscricaoController(InscricaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscricaoResponse cadastrar(@RequestBody InscricaoRequest dados) {
        return service.cadastrar(dados);
    }

    @GetMapping
    public List<InscricaoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/resumo")
    public ResumoResponse resumo() {
        return service.resumo();
    }

    @GetMapping("/{email}")
    public InscricaoResponse consultar(@PathVariable String email) {
        return service.consultar(email);
    }

    @PutMapping("/{email}")
    public InscricaoResponse editar(@PathVariable String email, @RequestBody InscricaoRequest dados) {
        return service.editar(email, dados);
    }

    @DeleteMapping("/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable String email) {
        service.cancelar(email);
    }
}
