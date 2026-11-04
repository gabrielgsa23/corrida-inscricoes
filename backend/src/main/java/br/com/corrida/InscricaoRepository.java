package br.com.corrida;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso ao banco. É só uma interface: o Spring Data escreve a implementação sozinho.
 *
 * JpaRepository já traz save, findAll, delete etc. Os métodos abaixo são "query methods":
 * o Spring lê o NOME do método e monta o SQL. Por exemplo:
 *   findByEmail          -> SELECT ... WHERE email = ?
 *   findAllByOrderByIdAsc -> SELECT ... ORDER BY id ASC
 */
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    Optional<Inscricao> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Todas as inscrições na ordem de cadastro. */
    List<Inscricao> findAllByOrderByIdAsc();
}
