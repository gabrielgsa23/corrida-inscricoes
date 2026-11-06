package br.com.corrida;

import br.com.corrida.validator.InscricaoValidator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Ponto de partida da aplicação (o "main").
 *
 * @SpringBootApplication liga a configuração automática do Spring Boot e faz o Spring
 * procurar classes anotadas (@RestController, @Service...) neste pacote e nos subpacotes
 * (controller, service, repository...). Por isso esta classe fica na raiz br.com.corrida.
 */
@SpringBootApplication
public class CorridaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorridaApplication.class, args);
    }

    /**
     * Cria o validador com a data da prova e o fuso do application.properties.
     *
     * Um método @Bean diz ao Spring: "quando alguém precisar de InscricaoValidator, use este".
     * Fazemos isso aqui (e não com @Component no validador) para que o InscricaoValidator
     * continue sendo Java puro, testável com um simples "new".
     *
     * @Value("${...}") lê a propriedade. LocalDate.parse falha logo na subida se a data
     * estiver errada no arquivo, o que é melhor do que descobrir no meio de um cadastro.
     */
    @Bean
    public InscricaoValidator inscricaoValidator(
            @Value("${corrida.data-prova}") String dataProva,
            @Value("${corrida.fuso}") String fuso) {
        return new InscricaoValidator(LocalDate.parse(dataProva), Clock.system(ZoneId.of(fuso)));
    }
}
