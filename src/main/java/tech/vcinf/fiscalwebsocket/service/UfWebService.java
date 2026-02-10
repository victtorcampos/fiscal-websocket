package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class UfWebService {
    private final Properties properties = new Properties();

    public UfWebService() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sefaz-urls.ini")) {
            if (input == null) {
                // Handle the case where the file is not found
                // For example, log an error or throw an exception
                System.err.println("Unable to find sefaz-urls.ini");
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUrl(String uf, String servico) {
        return properties.getProperty(servico.toUpperCase() + "." + uf.toUpperCase());
    }
}
