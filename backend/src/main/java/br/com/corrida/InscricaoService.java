package br.com.corrida;

import br.com.corrida.Contrato.DadosInscricao;
import br.com.corrida.Contrato.InscricaoResposta;
import br.com.corrida.Contrato.Resumo;
import br.com.corrida.RegrasDaInscricao.DadosValidos;
import br.com.corrida.RegrasDaInscricao.Percurso;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso da API: cadastrar, listar, consultar, editar, cancelar e resumir.
 *
 * O service é o "meio de campo": recebe os dados do controller, pede para as
 * RegrasDaInscricao validarem, conversa com o banco pelo repository e devolve
 * a resposta no formato do contrato. O controller não sabe de regra nenhuma.
 *
 * @Service registra a classe no Spring, que cria UM objeto dela e injeta onde for pedido.
 *
 * @Transactional na classe: cada método público roda dentro de uma transação.
 * Se qualquer RuntimeException for lançada no meio (ex.: InscricaoRecusada),
 * tudo o que o método fez no banco é desfeito (rollback). É uma das garantias de
 * que uma edição recusada não altera nada.
 */
@Service
@Transactional
public class InscricaoService {

    private final InscricaoRepository repositorio;
    private final RegrasDaInscricao regras;

    // Injeção de dependência pelo construtor: o Spring vê que precisa de um
    // repository e das regras, e passa os dois aqui. Não usamos "new" em lugar nenhum.
    public InscricaoService(InscricaoRepository repositorio, RegrasDaInscricao regras) {
        this.repositorio = repositorio;
        this.regras = regras;
    }

    /** POST: valida tudo, recusa e-mail repetido (409) e grava no fim da lista. */
    public InscricaoResposta cadastrar(DadosInscricao dados) {
        DadosValidos validos = regras.validar(
                dados.nome(), dados.email(), dados.dataNascimento(), dados.percurso());

        // O e-mail já está normalizado aqui, então " Ana@Email.COM " e "ana@email.com"
        // caem na mesma verificação.
        if (repositorio.existsByEmail(validos.email())) {
            throw InscricaoRecusada.emailEmUso();
        }

        Inscricao salva = repositorio.save(new Inscricao(validos));
        return paraResposta(salva);
    }

    /** GET da lista: em ordem de cadastro; lista vazia vira [] no JSON. */
    @Transactional(readOnly = true)
    public List<InscricaoResposta> listar() {
        return repositorio.findAllByOrderByIdAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    /** GET de uma inscrição pelo e-mail da URL. */
    @Transactional(readOnly = true)
    public InscricaoResposta consultar(String email) {
        return paraResposta(buscar(email));
    }

    /**
     * PUT: altera nome, nascimento e percurso. O e-mail vem da URL e nunca muda.
     *
     * Por que a edição recusada não altera nada: a validação acontece ANTES de
     * mexer no objeto. Se falhar, a exceção sai do método antes do atualizar(),
     * e ainda por cima o @Transactional faria rollback.
     *
     * Reaproveita o mesmo regras.validar() do cadastro. Passamos o e-mail já salvo
     * (e não o do corpo), então o que o cliente mandar no campo email é ignorado.
     */
    public InscricaoResposta editar(String email, DadosInscricao dados) {
        Inscricao inscricao = buscar(email); // 404 vem antes de qualquer validação

        DadosValidos validos = regras.validar(
                dados.nome(), inscricao.getEmail(), dados.dataNascimento(), dados.percurso());

        inscricao.atualizar(validos);
        // O id continua o mesmo, então a posição na lista e o total não mudam.
        // Se o percurso mudou, o resumo "move" sozinho, porque é contado a partir da lista.
        return paraResposta(repositorio.save(inscricao));
    }

    /** DELETE: remove pelo e-mail. Depois disso, o e-mail fica livre para recadastro. */
    public void cancelar(String email) {
        repositorio.delete(buscar(email));
    }

    /**
     * Resumo derivado da própria lista: não existe contador guardado em lugar nenhum,
     * então ele nunca fica fora de sincronia com as inscrições (inclusive zerado).
     */
    @Transactional(readOnly = true)
    public Resumo resumo() {
        List<Inscricao> todas = repositorio.findAll();
        long km5 = todas.stream().filter(i -> i.getPercurso() == Percurso.KM_5).count();
        long km10 = todas.stream().filter(i -> i.getPercurso() == Percurso.KM_10).count();
        return new Resumo(todas.size(), km5, km10);
    }

    // ------------------------------------------------------------------

    /**
     * Procura pelo e-mail normalizado: GET /api/inscricoes/ANA@Email.com encontra
     * a inscrição de ana@email.com. Se não existir, 404.
     */
    private Inscricao buscar(String email) {
        return repositorio.findByEmail(RegrasDaInscricao.normalizarEmail(email))
                .orElseThrow(InscricaoRecusada::naoEncontrada);
    }

    /** Monta o JSON de resposta. É aqui que idade e categoria são calculadas, sempre. */
    private InscricaoResposta paraResposta(Inscricao inscricao) {
        int idade = regras.idadeNaProva(inscricao.getDataNascimento());
        return new InscricaoResposta(
                inscricao.getNome(),
                inscricao.getEmail(),
                inscricao.getDataNascimento().toString(),
                inscricao.getPercurso(),
                idade,
                RegrasDaInscricao.categoriaPorIdade(idade));
    }
}
