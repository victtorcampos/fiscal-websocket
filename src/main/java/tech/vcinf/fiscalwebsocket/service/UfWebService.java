package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Service
public class UfWebService {
    private final Properties properties = new Properties();

    public UfWebService() {
        try {
            properties.load(new FileReader("src/main/resources/sefaz-urls.ini"));
        } catch (IOException e) {
            // In a real application, you'd want to handle this more gracefully
            throw new RuntimeException("Failed to load sefaz-urls.ini", e);
        }
    }

    public String getUrl(String modelo, String servico, String uf, String ambiente) {
        String key = String.format("%s.%s.%s.%s", modelo, servico, uf, ambiente).toUpperCase();
        String url = properties.getProperty(key);

        if (url == null) {
            throw new RuntimeException("URL não encontrada para a chave: " + key);
        }
        
        return url;
    }
}
