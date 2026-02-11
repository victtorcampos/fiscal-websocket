package tech.vcinf.fiscalwebsocket.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tech.vcinf.fiscalwebsocket.util.CacertUtil;

@Component
public class CacertInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("---------------------------------------------------");
        System.out.println("INICIALIZANDO CONFIGURAÇÃO DE CERTIFICADOS ICP-BRASIL");
        System.out.println("---------------------------------------------------");
        CacertUtil.instalarCertificadosICPBrasil();
        System.out.println("---------------------------------------------------");
    }
}
