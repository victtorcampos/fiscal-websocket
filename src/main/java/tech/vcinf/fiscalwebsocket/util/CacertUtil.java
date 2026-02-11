package tech.vcinf.fiscalwebsocket.util;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

public class CacertUtil {

    private static final List<String> ICP_BRASIL_URLS = Arrays.asList(
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv10.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv5.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv2.crt"
    );

    private static final String CACERT_FILE_NAME = "src/main/resources/cacert";
    private static final String CACERT_PASSWORD = "changeit";

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
        keyStore.load(null, CACERT_PASSWORD.toCharArray());

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        for (String urlString : ICP_BRASIL_URLS) {
            try {
                System.out.println("Baixando certificado raiz ICP-Brasil de: " + urlString);
                URL certUrl = new URL(urlString);
                try (InputStream in = certUrl.openStream()) {
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                    String alias = "icp-brasil-" + urlString.substring(urlString.lastIndexOf('/') + 1, urlString.lastIndexOf('.')).toLowerCase();
                    keyStore.setCertificateEntry(alias, cert);
                    System.out.println("Certificado ICP-Brasil adicionado: " + alias);
                }
            } catch (Exception e) {
                System.err.println("AVISO: Falha ao baixar certificado ICP-Brasil de " + urlString);
            }
        }

        System.out.println("\n--- Extraindo Certificados dos Servidores SEFAZ ---");
        extractCertificatesFromUrls(keyStore, "src/main/resources/sefaz-urls.ini");
        System.out.println("--- Extração de Certificados SEFAZ Concluída ---\n");

        try (FileOutputStream fos = new FileOutputStream(cacertFile)) {
            keyStore.store(fos, CACERT_PASSWORD.toCharArray());
            System.out.println("Arquivo 'cacert' criado com sucesso em: " + cacertFile.getAbsolutePath());
        }
    }

    private static void extractCertificatesFromUrls(KeyStore keyStore, String iniFilePath) throws Exception {
        File iniFile = new File(iniFilePath);
        if (!iniFile.exists()) {
            System.err.println("AVISO: Arquivo sefaz-urls.ini não encontrado. Pulando download de certificados SEFAZ.");
            return;
        }

        Set<String> hosts = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(iniFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                    continue;
                }
                if (line.contains("=")) {
                    String url = line.substring(line.indexOf('=') + 1).trim();
                    if (url.startsWith("https://")) {
                        try {
                            URL parsedUrl = new URL(url);
                            hosts.add(parsedUrl.getHost());
                        } catch (Exception e) {
                            System.err.println("AVISO: URL inválida ignorada: " + url);
                        }
                    }
                }
            }
        }

        System.out.println("Total de hosts SEFAZ únicos encontrados: " + hosts.size());

        for (String host : hosts) {
            try {
                 System.out.println("Extraindo certificados de: " + host);
                extractCertificatesViaSocket(keyStore, host);
            } catch (Exception e) {
                System.err.println("AVISO: Falha ao extrair certificados de " + host + ". Erro: " + e.getMessage());
            }
        }
    }

    private static void extractCertificatesViaSocket(KeyStore keyStore, String host) throws Exception {
        SSLSocketFactory factory = createTrustAllSSLContext().getSocketFactory();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, 443)) {
            socket.setSoTimeout(10000); // 10 segundos de timeout para leitura
            socket.startHandshake();

            Certificate[] serverCerts = socket.getSession().getPeerCertificates();

            for (int i = 0; i < serverCerts.length; i++) {
                if (serverCerts[i] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) serverCerts[i];

                    String hostAlias = host.replace(".", "_");
                    String alias = "sefaz-" + hostAlias + "-cert" + i;

                    if (!keyStore.containsAlias(alias)) {
                        keyStore.setCertificateEntry(alias, cert);
                        System.out.println("  → Certificado adicionado: " + alias);
                        System.out.println("     Subject: " + cert.getSubjectX500Principal().getName());
                    }
                }
            }
        }
    }

    private static SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc;
    }

    public static TrustManager[] getTrustManagers() throws Exception {
        File cacertFile = new File(CACERT_FILE_NAME);
        KeyStore customTrustStore = KeyStore.getInstance("JKS");
        if (cacertFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cacertFile)) {
                customTrustStore.load(fis, CACERT_PASSWORD.toCharArray());
            }
        } else {
            System.err.println("AVISO: Arquivo 'cacert' customizado não encontrado. Usando apenas o truststore padrão da JVM.");
            customTrustStore.load(null, CACERT_PASSWORD.toCharArray());
        }

        KeyStore defaultTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        String defaultTruststorePath = System.getProperty("java.home") + "/lib/security/cacerts";
        try (FileInputStream fis = new FileInputStream(defaultTruststorePath)) {
            defaultTrustStore.load(fis, "changeit".toCharArray());
        }

        KeyStore combinedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        combinedTrustStore.load(null, null);

        Enumeration<String> aliases = customTrustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = customTrustStore.getCertificate(alias);
            if (cert != null) {
                combinedTrustStore.setCertificateEntry(alias, cert);
            }
        }

        aliases = defaultTrustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!combinedTrustStore.containsAlias(alias)) {
                Certificate cert = defaultTrustStore.getCertificate(alias);
                if (cert != null) {
                    combinedTrustStore.setCertificateEntry("default-" + alias, cert);
                }
            }
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(combinedTrustStore);

        System.out.println("TrustManagers Híbrido (Custom + JVM Padrão) criado com sucesso.");
        return tmf.getTrustManagers();
    }
}
