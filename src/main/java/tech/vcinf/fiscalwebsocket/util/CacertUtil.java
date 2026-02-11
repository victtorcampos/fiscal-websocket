package tech.vcinf.fiscalwebsocket.util;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class CacertUtil {

    private static final List<String> ICP_BRASIL_URLS = Arrays.asList(
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv10.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv5.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv2.crt"
    );

    private static final String CACERT_FILE_NAME = "src/main/resources/cacert";
    private static final String CACERT_PASSWORD = "changeit";

    /**
     * Ponto de entrada chamado na inicialização da aplicação.
     * Garante que o arquivo cacert exista e define as propriedades do sistema.
     */
    public static void instalarCertificadosICPBrasil() {
        try {
            File cacertFile = new File(CACERT_FILE_NAME);

            if (!cacertFile.exists()) {
                System.out.println("Arquivo 'cacert' não encontrado. Baixando e criando um novo...");
                createCacertFile(cacertFile);
            } else {
                System.out.println("Arquivo 'cacert' encontrado. Carregando...");
            }

        } catch (Exception e) {
            System.err.println("AVISO: Falha ao inicializar o 'cacert'. Conexões SSL podem não funcionar como esperado.");
            e.printStackTrace();
        }
    }

    private static void createCacertFile(File cacertFile) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, CACERT_PASSWORD.toCharArray()); // Inicializa um keystore vazio

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        for (String urlString : ICP_BRASIL_URLS) {
            try {
                System.out.println("Baixando certificado de: " + urlString);
                URL certUrl = new URL(urlString);
                try (InputStream in = certUrl.openStream()) {
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                    String alias = "icp-brasil-" + urlString.substring(urlString.lastIndexOf('/') + 1, urlString.lastIndexOf('.')).toLowerCase();
                    keyStore.setCertificateEntry(alias, cert);
                    System.out.println("Certificado adicionado com o alias: " + alias);
                }
            } catch (Exception e) {
                System.err.println("AVISO: Falha ao baixar ou processar o certificado de " + urlString + ". " + e.getMessage());
                // Continua para o próximo certificado, não bloqueia a execução
            }
        }

        try (FileOutputStream fos = new FileOutputStream(cacertFile)) {
            keyStore.store(fos, CACERT_PASSWORD.toCharArray());
            System.out.println("Arquivo 'cacert' criado com sucesso em: " + cacertFile.getAbsolutePath());
        }
    }
    
    public static TrustManager[] getTrustManagers() throws Exception {
        // 1. Carregar o cacert customizado
        File cacertFile = new File(CACERT_FILE_NAME);
        KeyStore customTrustStore = KeyStore.getInstance("JKS");
        if (cacertFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cacertFile)) {
                customTrustStore.load(fis, CACERT_PASSWORD.toCharArray());
            }
        } else {
            System.err.println("AVISO: Arquivo 'cacert' customizado não encontrado. Usando apenas o truststore padrão da JVM.");
            customTrustStore.load(null, CACERT_PASSWORD.toCharArray()); // Inicia vazio
        }

        // 2. Carregar o truststore padrão da JVM
        KeyStore defaultTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        String defaultTruststorePath = System.getProperty("java.home") + "/lib/security/cacerts";
        try (FileInputStream fis = new FileInputStream(defaultTruststorePath)) {
            defaultTrustStore.load(fis, "changeit".toCharArray());
        }

        // 3. Combinar os dois truststores
        KeyStore combinedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        combinedTrustStore.load(null, null); // Inicia vazio

        // Adiciona certificados do customizado
        Enumeration<String> aliases = customTrustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = customTrustStore.getCertificate(alias);
            if (cert != null) {
                combinedTrustStore.setCertificateEntry(alias, cert);
            }
        }

        // Adiciona certificados do padrão
        aliases = defaultTrustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!combinedTrustStore.containsAlias(alias)) { // Evita duplicatas se já veio do customizado
                Certificate cert = defaultTrustStore.getCertificate(alias);
                if (cert != null) {
                    combinedTrustStore.setCertificateEntry("default-" + alias, cert);
                }
            }
        }

        // 4. Criar TrustManagerFactory com o truststore combinado
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(combinedTrustStore);

        System.out.println("TrustManagers Híbrido (Custom + JVM Padrão) criado com sucesso.");
        return tmf.getTrustManagers();
    }
}
