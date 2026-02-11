package tech.vcinf.fiscalwebsocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.vcinf.fiscalwebsocket.builder.ConsSitNFeBuilder;
import tech.vcinf.fiscalwebsocket.builder.ConsStatServBuilder;
import tech.vcinf.fiscalwebsocket.builder.InutNFeBuilder;
import tech.vcinf.fiscalwebsocket.builder.XmlBuilder;

import java.util.Map;

@Configuration
public class BuilderConfig {

    @Bean
    public Map<String, XmlBuilder> xmlBuilders() {
        return Map.of(
            "STATUS", new ConsStatServBuilder(),
            "CONSULTA", new ConsSitNFeBuilder(),
            "INUTILIZACAO", new InutNFeBuilder()
        );
    }
}
