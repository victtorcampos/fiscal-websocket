package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;
import tech.vcinf.fiscalwebsocket.model.Emitente;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CertificateManager {

    private final Map<String, KeyStore> keyStoreCache = new ConcurrentHashMap<>();

    public KeyStore getKeyStore(Emitente emitente) throws Exception {
        return keyStoreCache.computeIfAbsent(emitente.getCnpj(), cnpj -> {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new FileInputStream(emitente.getCaminhoCertificado()), emitente.getSenha().toCharArray());
                return keyStore;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load KeyStore for CNPJ: " + cnpj, e);
            }
        });
    }

    public KeyManagerFactory getKeyManagerFactory(Emitente emitente) throws Exception {
        KeyStore keyStore = getKeyStore(emitente);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, emitente.getSenha().toCharArray());
        return keyManagerFactory;
    }
}
