package tech.vcinf.fiscalwebsocket.service;

import org.apache.commons.httpclient.protocol.Protocol;
import org.springframework.stereotype.Service;
import tech.vcinf.fiscalwebsocket.model.Emitente;
import tech.vcinf.fiscalwebsocket.util.SocketFactoryDinamico;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SefazProtocolFactory {

    private final CertificateManager certificateManager;
    private final Map<String, Protocol> protocolCache = new ConcurrentHashMap<>();

    public SefazProtocolFactory(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    public Protocol getProtocol(Emitente emitente) throws Exception {
        return protocolCache.computeIfAbsent(emitente.getCnpj(), cnpj -> {
            try {
                // 1. Carregar KeyStore do certificado do emitente
                KeyStore keyStore = certificateManager.getKeyStore(emitente);
                String alias = keyStore.aliases().nextElement(); // Pega o primeiro alias

                // 2. Carregar o cacert customizado
                File cacertFile = new File("src/main/resources/cacert");
                KeyStore cacert = KeyStore.getInstance("JKS");
                try (FileInputStream fis = new FileInputStream(cacertFile)) {
                    cacert.load(fis, "changeit".toCharArray());
                }

                // 3. Criar SocketFactory com SSLContext configurado
                SocketFactoryDinamico socketFactory = new SocketFactoryDinamico(
                    keyStore,
                    alias,
                    emitente.getSenha(),
                    cacert,
                    "TLSv1.2"
                );

                // 4. Retornar o Protocol do Commons HttpClient
                return new Protocol("https", socketFactory, 443);

            } catch (Exception e) {
                throw new RuntimeException("Falha ao criar Protocol para CNPJ: " + cnpj, e);
            }
        });
    }
}
