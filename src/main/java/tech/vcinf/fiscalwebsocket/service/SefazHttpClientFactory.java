package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;
import tech.vcinf.fiscalwebsocket.model.Emitente;
import tech.vcinf.fiscalwebsocket.util.CacertUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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

                // CORREÇÃO: Obter TrustManagers Híbridos do CacertUtil
                TrustManager[] trustManagers = CacertUtil.getTrustManagers();

                SSLContext sslContext = SSLContext.getInstance("TLS");
                // Inicializa o SSLContext com os KeyManagers (nosso certificado) e os TrustManagers (certificados confiáveis)
                sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagers, // AGORA COM TRUSTMANAGERS CUSTOMIZADOS
                    null
                );

                return HttpClient.newBuilder()
                        .sslContext(sslContext)
                        .build();
            } catch (Exception e) {
                // Lança uma exceção mais específica para facilitar o debug
                throw new RuntimeException("Falha ao criar HttpClient para o CNPJ: " + cnpj, e);
            }
        });
    }
}
