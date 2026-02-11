package tech.vcinf.fiscalwebsocket.builder;

import java.util.Map;

public class ConsSitNFeBuilder implements XmlBuilder {
    @Override
    public String build(Map<String, Object> data, String cnpj, String uf) {
        String ambiente = (String) data.getOrDefault("ambiente", "1");
        String chave = (String) data.get("chNFe");
        String versao = "4.00";
        return String.format(
            "<consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"%s\">" +
            "<tpAmb>%s</tpAmb>" +
            "<xServ>CONSULTAR</xServ>" +
            "<chNFe>%s</chNFe>" +
            "</consSitNFe>",
            versao, ambiente, chave
        );
    }
}
