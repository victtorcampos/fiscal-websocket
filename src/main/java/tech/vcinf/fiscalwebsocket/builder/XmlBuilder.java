package tech.vcinf.fiscalwebsocket.builder;

import java.util.Map;

public interface XmlBuilder {
    String build(Map<String, Object> data, String cnpj, String uf);
}
