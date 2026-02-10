package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Service
public class UfWebService {
    private final Properties properties = new Properties();

    public UfWebService() {
        try (FileReader reader = new FileReader("src/main/resources/sefaz-urls.ini")) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUrl(String uf, String servico) {
        return properties.getProperty(servico.toUpperCase() + "." + uf.toUpperCase());
    }
}
