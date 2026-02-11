package tech.vcinf.fiscalwebsocket.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CacertUtil {

    private static final String CACERT_FILE_NAME = "cacert";
    private static final String CACERT_PASSWORD = "changeit";
    private static final String[] ICP_BRASIL_URLS = {
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv10.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv5.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv2.crt"
    };

    public static void instalarCertificadosICPBrasil() {
        try {
            // O arquivo será salvo no diretório de trabalho, que é a raiz do projeto.
            File cacertFile = new File(CACERT_FILE_NAME);

            if (!cacertFile.exists()) {
                System.out.println("Arquivo 'cacert' não encontrado na raiz do projeto. Criando um novo...");
                createCacertFile(cacertFile);
            } else {
                System.out.println("Arquivo 'cacert' encontrado. Carregando...");
            }

            // Define as propriedades de sistema para a JVM usar este truststore
            System.setProperty("javax.net.ssl.trustStore", cacertFile.getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", CACERT_PASSWORD);
            System.out.println("TrustStore da JVM configurado para usar: " + cacertFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("ERRO CRÍTICO ao configurar o 'cacert'. As conexões SSL podem falhar.");
            e.printStackTrace();
        }
    }

    private static void createCacertFile(File cacertFile) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, CACERT_PASSWORD.toCharArray());

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (String urlString : ICP_BRASIL_URLS) {
                try {
                    System.out.println("Baixando certificado de: " + urlString);
                    URL certUrl = new URL(urlString);
                    try (InputStream in = certUrl.openStream()) {
                        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                        // Extrai um alias amigável da URL
                        String alias = "icp-brasil-" + urlString.substring(urlString.lastIndexOf('/') + 1, urlString.lastIndexOf('.')).toLowerCase();
                        keyStore.setCertificateEntry(alias, cert);
                        System.out.println("Certificado adicionado com o alias: " + alias);
                    }
                } catch (Exception e) {
                    System.err.println("AVISO: Falha ao baixar ou processar o certificado de " + urlString + ". " + e.getMessage());
                    // Continua para o próximo certificado, não bloqueia a execução
                }
            }

            // Salva o novo keystore no arquivo
            try (FileOutputStream fos = new FileOutputStream(cacertFile)) {
                keyStore.store(fos, CACERT_PASSWORD.toCharArray());
                System.out.println("Arquivo 'cacert' criado com sucesso em: " + cacertFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("ERRO FATAL: Não foi possível criar o arquivo 'cacert'.");
            e.printStackTrace();
        }
    }
}
