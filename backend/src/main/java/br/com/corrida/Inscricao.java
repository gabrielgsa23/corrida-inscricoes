package br.com.corrida;

import br.com.corrida.RegrasDaInscricao.DadosValidos;
import br.com.corrida.RegrasDaInscricao.Percurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

/**
 * Entidade JPA: cada objeto Inscricao é uma linha da tabela "inscricao" no H2.
 * O Hibernate cria a tabela sozinho a partir desta classe (ddl-auto no application.properties).
 *
 * Só os quatro campos de entrada são gravados. Idade na prova e categoria NÃO são colunas:
 * são calculadas na hora de montar a resposta, a partir do nascimento. Assim, se a data
 * da prova mudar, todo mundo é recalculado automaticamente e nada fica desatualizado.
 */
@Entity
public class Inscricao {

    /**
     * Chave técnica gerada pelo banco: 1, 2, 3... na ordem de cadastro.
     * É ela que garante a ordem da listagem (ORDER BY id). Na edição o id não muda,
     * então a inscrição continua na mesma posição. Um recadastro depois de cancelar
     * ganha id novo e vai para o fim, como pede o enunciado.
     * O id nunca aparece no JSON: para o cliente, quem identifica é o e-mail.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /**
     * Chave de negócio: já chega normalizado (minúsculas, sem espaços nas pontas).
     * unique = true cria uma restrição no banco, que é a última barreira contra e-mail repetido.
     */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    // LocalDate vira uma coluna DATE (só dia, sem hora e sem fuso).
    @Column(nullable = false)
    private LocalDate dataNascimento;

    // EnumType.STRING grava o texto "KM_5"/"KM_10". O padrão (ORDINAL) gravaria 0/1,
    // que quebraria os dados se alguém mudasse a ordem do enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Percurso percurso;

    /** Construtor vazio exigido pelo JPA para montar o objeto ao ler do banco. */
    protected Inscricao() {
    }

    /** Nova inscrição, sempre a partir de dados já validados pelas regras. */
    public Inscricao(DadosValidos dados) {
        this.email = dados.email();
        atualizar(dados);
    }

    /**
     * Usado na edição. Repare que o e-mail NÃO é alterado aqui: ele é imutável.
     * Não existe setEmail nesta classe, então não tem como trocá-lo por engano.
     */
    public void atualizar(DadosValidos dados) {
        this.nome = dados.nome();
        this.dataNascimento = dados.dataNascimento();
        this.percurso = dados.percurso();
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public Percurso getPercurso() {
        return percurso;
    }
}
