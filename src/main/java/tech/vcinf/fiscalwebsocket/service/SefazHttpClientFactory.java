package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;
import tech.vcinf.fiscalwebsocket.model.Emitente;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SefazHttpClientFactory {

    private final CertificateManager certificateManager;
    private final Map<String, HttpClient> clientCache = new ConcurrentHashMap<>();

    public SefazHttpClientFactory(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    public HttpClient getHttpClient(Emitente emitente) throws Exception {
        return clientCache.computeIfAbsent(emitente.getCnpj(), cnpj -> {
            try {
                KeyManagerFactory keyManagerFactory = certificateManager.getKeyManagerFactory(emitente);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), null, null); // Trust managers are not needed for mTLS client auth

                return HttpClient.newBuilder()
                        .sslContext(sslContext)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create HttpClient for CNPJ: " + cnpj, e);
            }
        });
    }
}
