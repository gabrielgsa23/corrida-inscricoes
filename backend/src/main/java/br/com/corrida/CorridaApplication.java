package br.com.corrida;

import br.com.corrida.validator.InscricaoValidator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CorridaApplication {
    public static void main(String[] args) {
        SpringApplication.run(CorridaApplication.class, args);
    }

    @Bean
    public InscricaoValidator inscricaoValidator(
            @Value("${corrida.data-prova}") String dataProva,
            @Value("${corrida.fuso}") String fuso) {
        return new InscricaoValidator(LocalDate.parse(dataProva), Clock.system(ZoneId.of(fuso)));
    }
}
