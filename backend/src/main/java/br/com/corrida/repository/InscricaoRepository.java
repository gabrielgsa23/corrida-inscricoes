package br.com.corrida.repository;

import br.com.corrida.enums.Percurso;
import br.com.corrida.model.Inscricao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso ao banco. É só uma interface: o Spring Data escreve a implementação sozinho.
 *
 * JpaRepository já traz save, findAll, count, delete etc. Os métodos abaixo são
 * "query methods": o Spring lê o NOME do método e monta o SQL. Por exemplo:
 *   findByEmail           -> SELECT ... WHERE email = ?
 *   countByPercurso       -> SELECT COUNT(*) ... WHERE percurso = ?
 *   findAllByOrderByIdAsc -> SELECT ... ORDER BY id ASC
 */
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    Optional<Inscricao> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByPercurso(Percurso percurso);

    /** Todas as inscrições na ordem de cadastro. */
    List<Inscricao> findAllByOrderByIdAsc();
}
