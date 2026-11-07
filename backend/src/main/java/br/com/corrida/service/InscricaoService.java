package br.com.corrida.service;

import br.com.corrida.dto.InscricaoRequest;
import br.com.corrida.dto.InscricaoResponse;
import br.com.corrida.dto.ResumoResponse;
import br.com.corrida.enums.Categoria;
import br.com.corrida.enums.Percurso;
import br.com.corrida.exception.EmailJaCadastradoException;
import br.com.corrida.exception.InscricaoNaoEncontradaException;
import br.com.corrida.model.Inscricao;
import br.com.corrida.repository.InscricaoRepository;
import br.com.corrida.validator.InscricaoValidator;
import br.com.corrida.validator.InscricaoValidator.DadosValidos;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InscricaoService {
    private final InscricaoRepository repository;
    private final InscricaoValidator validator;

    public InscricaoService(InscricaoRepository repository, InscricaoValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public InscricaoResponse cadastrar(InscricaoRequest dados) {
        DadosValidos validos = validator.validar(dados, dados.email());

        if (repository.existsByEmail(validos.email())) {
            throw new EmailJaCadastradoException();
        }

        Inscricao nova = new Inscricao(
                validos.nome(), validos.email(), validos.dataNascimento(), validos.percurso());
        return paraResposta(repository.save(nova));
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> listar() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public InscricaoResponse consultar(String email) {
        return paraResposta(buscar(email));
    }

    public InscricaoResponse editar(String email, InscricaoRequest dados) {
        Inscricao inscricao = buscar(email);

        DadosValidos validos = validator.validar(dados, inscricao.getEmail());

        inscricao.atualizar(validos.nome(), validos.dataNascimento(), validos.percurso());

        return paraResposta(repository.save(inscricao));
    }

    public void cancelar(String email) {
        repository.delete(buscar(email));
    }

    @Transactional(readOnly = true)
    public ResumoResponse resumo() {
        return new ResumoResponse(
                repository.count(),
                repository.countByPercurso(Percurso.KM_5),
                repository.countByPercurso(Percurso.KM_10));
    }

    private Inscricao buscar(String email) {
        return repository.findByEmail(InscricaoValidator.normalizarEmail(email))
                .orElseThrow(InscricaoNaoEncontradaException::new);
    }

    private InscricaoResponse paraResposta(Inscricao inscricao) {
        int idade = validator.idadeNaProva(inscricao.getDataNascimento());
        return new InscricaoResponse(
                inscricao.getNome(),
                inscricao.getEmail(),
                inscricao.getDataNascimento().toString(),
                inscricao.getPercurso(),
                idade,
                Categoria.porIdade(idade));
    }
}
