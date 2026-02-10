package tech.vcinf.fiscalwebsocket.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class CertificateUtils {

    public static X509Certificate getCertificate(InputStream pfxStream, String password) throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(pfxStream, password.toCharArray());
            return getCertificateFromKeyStore(keyStore);
        } catch (Exception e) {
            Security.addProvider(new BouncyCastleProvider());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(pfxStream, password.toCharArray());
            return getCertificateFromKeyStore(keyStore);
        }
    }

    private static X509Certificate getCertificateFromKeyStore(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        String alias = aliases.nextElement();
        return (X509Certificate) keyStore.getCertificate(alias);
    }

    public static Map<String, String> extractInfo(X509Certificate cert) {
        Map<String, String> info = new HashMap<>();
        info.put("validade", cert.getNotAfter().toString());
        
        try {
            LdapName ldapName = new LdapName(cert.getSubjectDN().getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    String cn = rdn.getValue().toString();
                    String[] cnParts = cn.split(":");
                    if (cnParts.length >= 2) {
                        info.put("razaoSocial", cnParts[0]);
                        info.put("cnpj", cnParts[1]);
                    } else {
                        info.put("razaoSocial", cn);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar SubjectDN do certificado", e);
        }
        return info;
    }
}
