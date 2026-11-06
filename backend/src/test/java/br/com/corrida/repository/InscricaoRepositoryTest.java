package br.com.corrida.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.corrida.enums.Percurso;
import br.com.corrida.model.Inscricao;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Testa só a camada de banco, no próprio H2 em memória.
 *
 * @DataJpaTest sobe apenas o JPA e o repository (sem controller nem service) e desfaz
 * tudo ao fim de cada teste, então um teste não enxerga o que o outro gravou.
 */
@DataJpaTest
class InscricaoRepositoryTest {

    @Autowired
    private InscricaoRepository repository;

    @Test
    void buscaPorEmailEConfereSeExiste() {
        repository.save(inscricao("ana@email.com", Percurso.KM_5));

        assertTrue(repository.findByEmail("ana@email.com").isPresent());
        assertTrue(repository.existsByEmail("ana@email.com"));
        assertFalse(repository.existsByEmail("bruno@email.com"));
    }

    @Test
    void listaNaOrdemDeCadastroEContaPorPercurso() {
        repository.save(inscricao("ana@email.com", Percurso.KM_10));
        repository.save(inscricao("bruno@email.com", Percurso.KM_5));
        repository.save(inscricao("carla@email.com", Percurso.KM_5));

        List<String> emails = repository.findAllByOrderByIdAsc().stream().map(Inscricao::getEmail).toList();

        assertEquals(List.of("ana@email.com", "bruno@email.com", "carla@email.com"), emails);
        assertEquals(2, repository.countByPercurso(Percurso.KM_5));
        assertEquals(1, repository.countByPercurso(Percurso.KM_10));
    }

    @Test
    void bancoRecusaEmailRepetido() {
        // A restrição unique da coluna é a última barreira, mesmo que o service falhe.
        repository.saveAndFlush(inscricao("ana@email.com", Percurso.KM_5));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(inscricao("ana@email.com", Percurso.KM_10)));
    }

    private static Inscricao inscricao(String email, Percurso percurso) {
        return new Inscricao("Ana Souza", email, LocalDate.of(1990, 5, 20), percurso);
    }
}
