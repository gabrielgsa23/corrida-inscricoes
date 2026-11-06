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

/**
 * Casos de uso da API: cadastrar, listar, consultar, editar, cancelar e resumir.
 *
 * O service é o "meio de campo": recebe o DTO do controller, pede para o validator
 * conferir, conversa com o banco pelo repository e devolve o DTO de resposta.
 *
 * @Service registra a classe no Spring, que cria UM objeto dela e injeta onde for pedido.
 *
 * @Transactional na classe: cada método público roda numa transação. Se uma exceção
 * for lançada no meio, tudo o que o método fez no banco é desfeito (rollback).
 */
@Service
@Transactional
public class InscricaoService {

    private final InscricaoRepository repository;
    private final InscricaoValidator validator;

    // Injeção de dependência pelo construtor: o Spring passa o repository e o validator.
    public InscricaoService(InscricaoRepository repository, InscricaoValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /** POST: valida tudo, recusa e-mail repetido (409) e grava no fim da lista. */
    public InscricaoResponse cadastrar(InscricaoRequest dados) {
        DadosValidos validos = validator.validar(dados, dados.email());

        // O e-mail já está normalizado, então " Ana@Email.COM " e "ana@email.com"
        // caem na mesma verificação.
        if (repository.existsByEmail(validos.email())) {
            throw new EmailJaCadastradoException();
        }

        Inscricao nova = new Inscricao(
                validos.nome(), validos.email(), validos.dataNascimento(), validos.percurso());
        return paraResposta(repository.save(nova));
    }

    /** GET da lista: em ordem de cadastro; lista vazia vira [] no JSON. */
    @Transactional(readOnly = true)
    public List<InscricaoResponse> listar() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    /** GET de uma inscrição pelo e-mail da URL. */
    @Transactional(readOnly = true)
    public InscricaoResponse consultar(String email) {
        return paraResposta(buscar(email));
    }

    /**
     * PUT: altera nome, nascimento e percurso. O e-mail vem da URL e nunca muda.
     *
     * O 404 vem antes de qualquer validação. Depois, o mesmo validar() do cadastro
     * recebe o e-mail JÁ GRAVADO, então o e-mail do corpo é ignorado.
     * Se a validação falhar, a exceção sai antes do atualizar(): nada é alterado.
     */
    public InscricaoResponse editar(String email, InscricaoRequest dados) {
        Inscricao inscricao = buscar(email);

        DadosValidos validos = validator.validar(dados, inscricao.getEmail());

        inscricao.atualizar(validos.nome(), validos.dataNascimento(), validos.percurso());
        // O id continua o mesmo, então a posição na lista e o total não mudam.
        return paraResposta(repository.save(inscricao));
    }

    /** DELETE: remove pelo e-mail. Depois disso, o e-mail fica livre para recadastro. */
    public void cancelar(String email) {
        repository.delete(buscar(email));
    }

    /**
     * Resumo contado direto no banco: não existe contador guardado em lugar nenhum,
     * então ele nunca fica fora de sincronia com as inscrições (inclusive zerado).
     */
    @Transactional(readOnly = true)
    public ResumoResponse resumo() {
        return new ResumoResponse(
                repository.count(),
                repository.countByPercurso(Percurso.KM_5),
                repository.countByPercurso(Percurso.KM_10));
    }

    // ------------------------------------------------------------------

    /**
     * Procura pelo e-mail normalizado: /api/inscricoes/ANA@Email.com encontra
     * a inscrição de ana@email.com. Se não existir, 404.
     */
    private Inscricao buscar(String email) {
        return repository.findByEmail(InscricaoValidator.normalizarEmail(email))
                .orElseThrow(InscricaoNaoEncontradaException::new);
    }

    /** Converte a entidade no DTO de resposta. É aqui que idade e categoria são calculadas. */
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
