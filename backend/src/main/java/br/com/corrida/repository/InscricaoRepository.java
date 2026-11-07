package br.com.corrida.repository;

import br.com.corrida.enums.Percurso;
import br.com.corrida.model.Inscricao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
    Optional<Inscricao> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByPercurso(Percurso percurso);

    List<Inscricao> findAllByOrderByIdAsc();
}
