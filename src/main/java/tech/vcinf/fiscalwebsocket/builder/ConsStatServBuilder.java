package tech.vcinf.fiscalwebsocket.builder;

import java.util.Map;
import java.util.UUID;

public class ConsStatServBuilder implements XmlBuilder {
    @Override
    public String build(Map<String, Object> data, String cnpj, String uf) {
        String ambiente = (String) data.getOrDefault("ambiente", "1"); // 1-Produção, 2-Homologação
        String versao = "4.00"; // Pode ser parametrizado se necessário
        return String.format(
            "<consStatServ xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"%s\">" +
            "<tpAmb>%s</tpAmb>" +
            "<cUF>%s</cUF>" +
            "<xServ>STATUS</xServ>" +
            "</consStatServ>",
            versao, ambiente, uf
        );
    }
}
