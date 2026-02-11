package tech.vcinf.fiscalwebsocket.util;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Utilitário para criar e gerenciar o truststore customizado (cacert)
 * com certificados ICP-Brasil e certificados SSL dos servidores SEFAZ.
 * 
 * Estratégia: Combina certificados raiz da ICP-Brasil com certificados
 * extraídos diretamente dos hosts SEFAZ listados no sefaz-urls.ini.
 */
public class CacertUtil {

    private static final List<String> ICP_BRASIL_URLS = Arrays.asList(
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv10.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv5.crt",
            "http://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv2.crt"
    );

    private static final String CACERT_FILE_NAME = "src/main/resources/cacert";
    private static final String CACERT_PASSWORD = "changeit";
    private static final int TIMEOUT_MS = 10000; // 10 segundos
    private static final int PORTA_SSL = 443;

    public static void instalarCertificadosICPBrasil() {
        try {
            System.out.println("\n=== Inicializando Configuração de Certificados ===");
            File cacertFile = new File(CACERT_FILE_NAME);
            
            if (!cacertFile.exists()) {
                System.out.println("Arquivo 'cacert' não encontrado. Criando novo...");
                createCacertFile(cacertFile);
            } else {
                System.out.println("Arquivo 'cacert' encontrado: " + cacertFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("ERRO CRÍTICO ao inicializar cacert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createCacertFile(File cacertFile) throws Exception {
        // 1. Inicializar KeyStore vazio
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, CACERT_PASSWORD.toCharArray());

        // 2. Baixar certificados raiz ICP-Brasil
        System.out.println("\n--- FASE 1: Certificados Raiz ICP-Brasil ---");
        downloadIcpBrasilCertificates(keyStore);

        // 3. Extrair certificados dos servidores SEFAZ
        System.out.println("\n--- FASE 2: Certificados dos Servidores SEFAZ ---");
        extractSefazCertificates(keyStore, "src/main/resources/sefaz-urls.ini");

        // 4. Salvar KeyStore no arquivo cacert
        try (FileOutputStream fos = new FileOutputStream(cacertFile)) {
            keyStore.store(fos, CACERT_PASSWORD.toCharArray());
            System.out.println("\n✓ Arquivo 'cacert' criado com sucesso: " + cacertFile.getAbsolutePath());
            System.out.println("✓ Total de certificados instalados: " + keyStore.size());
        }
    }

    private static void downloadIcpBrasilCertificates(KeyStore keyStore) {
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            System.err.println("ERRO: Não foi possível criar CertificateFactory");
            return;
        }

        for (String urlString : ICP_BRASIL_URLS) {
            try {
                System.out.println("Baixando: " + urlString);
                URL certUrl = new URL(urlString);
                
                try (InputStream in = certUrl.openStream()) {
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                    String alias = "icp-brasil-" + extractFilename(urlString);
                    keyStore.setCertificateEntry(alias, cert);
                    System.out.println("  ✓ Instalado: " + alias);
                }
            } catch (Exception e) {
                System.err.println("  ✗ Falha em " + urlString + ": " + e.getMessage());
            }
        }
    }

    private static void extractSefazCertificates(KeyStore keyStore, String iniFilePath) {
        File iniFile = new File(iniFilePath);
        if (!iniFile.exists()) {
            System.err.println("AVISO: Arquivo sefaz-urls.ini não encontrado. Pulando certificados SEFAZ.");
            return;
        }

        // Usar Set para evitar hosts duplicados
        Set<String> hosts = extractUniqueHosts(iniFile);
        System.out.println("Total de hosts únicos encontrados: " + hosts.size());

        int sucessos = 0;
        int falhas = 0;

        for (String host : hosts) {
            try {
                System.out.println("Extraindo certificados de: " + host);
                
                // Usar SavingTrustManager para capturar certificados mesmo com handshake failure
                SSLContext context = SSLContext.getInstance("TLS");
                SavingTrustManager trustManager = new SavingTrustManager(keyStore);
                context.init(null, new TrustManager[]{trustManager}, null);
                SSLSocketFactory factory = context.getSocketFactory();

                SSLSocket socket = (SSLSocket) factory.createSocket(host, PORTA_SSL);
                socket.setSoTimeout(TIMEOUT_MS);
                
                try {
                    socket.startHandshake();
                } catch (SSLException e) {
                    // Esperado - capturamos os certificados no SavingTrustManager
                } finally {
                    socket.close();
                }

                // Adicionar certificados capturados ao KeyStore
                X509Certificate[] chain = trustManager.getChain();
                if (chain != null && chain.length > 0) {
                    for (int i = 0; i < chain.length; i++) {
                        String alias = "sefaz-" + host.replace(".", "_") + "-cert" + i;
                        if (!keyStore.containsAlias(alias)) {
                            keyStore.setCertificateEntry(alias, chain[i]);
                            System.out.println("  ✓ Certificado " + i + ": " + 
                                chain[i].getSubjectX500Principal().getName().split(",")[0]);
                        }
                    }
                    sucessos++;
                } else {
                    System.err.println("  ✗ Nenhum certificado obtido");
                    falhas++;
                }

            } catch (Exception e) {
                System.err.println("  ✗ Erro: " + e.getMessage());
                falhas++;
            }
        }

        System.out.println("\nResumo: " + sucessos + " sucessos, " + falhas + " falhas");
    }

    private static Set<String> extractUniqueHosts(File iniFile) {
        Set<String> hosts = new LinkedHashSet<>(); // Mantém ordem de inserção
        
        try (BufferedReader reader = new BufferedReader(new FileReader(iniFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Ignorar linhas vazias, comentários e seções
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                    continue;
                }
                
                // Extrair URL (formato: chave=valor)
                if (line.contains("=")) {
                    String url = line.substring(line.indexOf('=') + 1).trim();
                    
                    if (url.startsWith("https://")) {
                        try {
                            URL parsedUrl = new URL(url);
                            hosts.add(parsedUrl.getHost()); // Adiciona apenas o host
                        } catch (Exception e) {
                            System.err.println("URL inválida ignorada: " + url);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler sefaz-urls.ini: " + e.getMessage());
        }
        
        return hosts;
    }

    private static String extractFilename(String url) {
        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        return filename.toLowerCase();
    }

    /**
     * Retorna TrustManagers que combinam o cacert customizado com o truststore padrão da JVM
     */
    public static TrustManager[] getTrustManagers() throws Exception {
        File cacertFile = new File(CACERT_FILE_NAME);
        
        // 1. Carregar cacert customizado
        KeyStore customTrustStore = KeyStore.getInstance("JKS");
        if (cacertFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cacertFile)) {
                customTrustStore.load(fis, CACERT_PASSWORD.toCharArray());
            }
        } else {
            System.err.println("AVISO: cacert não encontrado. Usando apenas truststore da JVM.");
            customTrustStore.load(null, CACERT_PASSWORD.toCharArray());
        }

        // 2. Carregar truststore padrão da JVM
        KeyStore defaultTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        String javaHome = System.getProperty("java.home");
        String cacertsPath = javaHome + "/lib/security/cacerts";
        
        try (FileInputStream fis = new FileInputStream(cacertsPath)) {
            defaultTrustStore.load(fis, "changeit".toCharArray());
        }

        // 3. Combinar ambos os truststores
        KeyStore combinedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        combinedTrustStore.load(null, null);

        // Adicionar certificados customizados
        Enumeration<String> aliases = customTrustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = customTrustStore.getCertificate(alias);
            if (cert != null) {
                combinedTrustStore.setCertificateEntry(alias, cert);
            }
        }

        // Adicionar certificados da JVM (evitando duplicatas)
        aliases = defaultTrustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!combinedTrustStore.containsAlias(alias)) {
                Certificate cert = defaultTrustStore.getCertificate(alias);
                if (cert != null) {
                    combinedTrustStore.setCertificateEntry("jvm-" + alias, cert);
                }
            }
        }

        // 4. Criar TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(combinedTrustStore);

        return tmf.getTrustManagers();
    }

    /**
     * TrustManager customizado que captura a cadeia de certificados
     * mesmo quando o handshake falha (útil para servidores com mTLS obrigatório)
     */
    private static class SavingTrustManager implements X509TrustManager {
        private final X509TrustManager defaultTrustManager;
        private X509Certificate[] chain;

        public SavingTrustManager(KeyStore keyStore) throws Exception {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            this.defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
        }

        public X509Certificate[] getChain() {
            return chain;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain; // Captura a cadeia ANTES de validar
            try {
                this.defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                // Ignora erro de validação - já capturamos os certificados
            }
        }
    }
}
