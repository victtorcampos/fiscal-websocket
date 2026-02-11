package tech.vcinf.fiscalwebsocket.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tech.vcinf.fiscalwebsocket.util.CacertUtil;

@Component
public class CacertInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Inicializando configuração de certificados ICP-Brasil ---");
        CacertUtil.instalarCertificadosICPBrasil();
        System.out.println("--- Configuração de certificados finalizada ---");
    }
}
