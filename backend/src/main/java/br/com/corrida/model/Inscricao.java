package br.com.corrida.model;

import br.com.corrida.enums.Percurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Inscricao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Percurso percurso;

    protected Inscricao() {
    }

    public Inscricao(String nome, String email, LocalDate dataNascimento, Percurso percurso) {
        this.nome = nome;
        this.email = email;
        this.dataNascimento = dataNascimento;
        this.percurso = percurso;
    }

    public void atualizar(String nome, LocalDate dataNascimento, Percurso percurso) {
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.percurso = percurso;
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
