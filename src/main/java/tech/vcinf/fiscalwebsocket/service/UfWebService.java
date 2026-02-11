package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class UfWebService {

    private final Map<String, Map<String, Map<String, Map<String, String>>>> operationsCache = new HashMap<>();

    @PostConstruct
    public void init() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/sefaz-urls.ini")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;

                String[] parts = line.split("=");
                String key = parts[0];
                String url = parts[1];

                String[] keyParts = key.split("\\.");
                if (keyParts.length == 4) {
                    String modelo = keyParts[0];
                    String servico = keyParts[1];
                    String uf = keyParts[2];
                    String ambiente = keyParts[3];

                    operationsCache
                        .computeIfAbsent(modelo, k -> new HashMap<>())
                        .computeIfAbsent(uf, k -> new HashMap<>())
                        .computeIfAbsent(ambiente, k -> new HashMap<>())
                        .put(servico, url);
                }
            }
        } catch (Exception e) {
            System.err.println("Falha ao carregar e processar o arquivo sefaz-urls.ini");
            e.printStackTrace();
        }
    }

    public String getUrl(String modelo, String servico, String uf, String ambiente) {
        Map<String, String> serviceUrls = operationsCache
                .getOrDefault(modelo, new HashMap<>())
                .getOrDefault(uf, new HashMap<>())
                .getOrDefault(ambiente, new HashMap<>());

        return serviceUrls.get(servico);
    }
}
