<!-- Instruções de Desenvolvimento: Implementação de XmlBuilders -->
# Implementação de XmlBuilders - Geração Automática de XMLs Fiscais

## 1. Contexto do Sistema

O `fiscal-websocket` atualmente **declara** a interface `XmlBuilder` e a injeta em `FiscalController`, mas **não possui implementações concretas**. Isso significa que:

- ✅ Para operações complexas (ex: `AUTORIZACAO`), o cliente envia XML completo
- ❌ Para operações simples (ex: `STATUS`, `CONSULTA`), o cliente AINDA precisa enviar XML completo (ineficiente)

### Problema Atual

```java
// FiscalController.java (linha ~115)
XmlBuilder builder = xmlBuilders.get(servico);
if (builder != null) {
    xml = builder.build(payload, cnpj, emitente.getUf());
} else {
    xml = request.getXml(); // ❌ Fallback: cliente envia XML
}
```

**Resultado:** `xmlBuilders` é sempre `null` porque nenhum bean implementa `XmlBuilder`.

---

## 2. Objetivo da Implementação

Criar **5 builders** que geram XMLs automaticamente para operações fiscais comuns, eliminando a necessidade do cliente construir XMLs manualmente.

### Builders Prioritários

| Builder | Serviço | Descrição | Complexidade |
|---------|---------|-----------|--------------|
| `ConsStatServBuilder` | `STATUS` | Consulta status do web service | ⭐ Simples |
| `ConsSitNFeBuilder` | `CONSULTA` | Consulta NF-e por chave de acesso | ⭐ Simples |
| `ConsultaCadastroBuilder` | `CONSULTA_CADASTRO` | Consulta cadastro de contribuinte | ⭐⭐ Média |
| `InutNFeBuilder` | `INUTILIZACAO` | Inutiliza faixa de numeração | ⭐⭐ Média |
| `EventoBuilder` | `EVENTO` | Eventos (cancelamento, CC-e) | ⭐⭐⭐ Complexa |

---

## 3. Arquitetura da Solução

### 3.1. Interface Base (Já Existe)

```java
package tech.vcinf.fiscalwebsocket.builder;

import java.util.Map;

public interface XmlBuilder {
    /**
     * Constrói XML para operação fiscal
     * @param payload Dados da operação (chave, justificativa, série, etc.)
     * @param cnpj CNPJ do emitente
     * @param uf UF do emitente
     * @return XML pronto para assinatura/transmissão
     */
    String build(Map<String, Object> payload, String cnpj, String uf);
}
```

### 3.2. Padrão de Implementação

Cada builder deve:
1. ✅ Ser um `@Component` Spring (para injeção automática)
2. ✅ Implementar `XmlBuilder`
3. ✅ Validar campos obrigatórios antes de gerar XML
4. ✅ Usar namespace XML correto do modelo fiscal
5. ✅ Adicionar declaração XML (`<?xml version="1.0" encoding="UTF-8"?>`)
6. ✅ Lançar `IllegalArgumentException` com mensagem clara em caso de erro

### 3.3. Configuração Spring (Nova Classe)

```java
package tech.vcinf.fiscalwebsocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.vcinf.fiscalwebsocket.builder.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class XmlBuilderConfig {

    @Bean
    public Map<String, XmlBuilder> xmlBuilders(
            ConsStatServBuilder statusBuilder,
            ConsSitNFeBuilder consultaBuilder,
            ConsultaCadastroBuilder cadastroBuilder,
            InutNFeBuilder inutilizacaoBuilder,
            EventoBuilder eventoBuilder) {
        
        Map<String, XmlBuilder> builders = new HashMap<>();
        builders.put("STATUS", statusBuilder);
        builders.put("CONSULTA", consultaBuilder);
        builders.put("CONSULTA_CADASTRO", cadastroBuilder);
        builders.put("INUTILIZACAO", inutilizacaoBuilder);
        builders.put("EVENTO", eventoBuilder);
        return builders;
    }
}
```

---

## 4. Especificações Técnicas por Builder

### 4.1. ConsStatServBuilder (STATUS)

#### Campos Obrigatórios no Payload
- `ambiente` (String): "PROD" ou "HOMOL" (converte para tpAmb: 1=Produção, 2=Homologação)
- `uf` (String): UF do emitente (injetado automaticamente)

#### Estrutura XML Esperada (NFe)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<consStatServ xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
    <tpAmb>1</tpAmb>
    <cUF>51</cUF>
    <xServ>STATUS</xServ>
</consStatServ>
```

#### Mapeamento UF → Código IBGE
| UF | Código |
|----|--------|
| MT | 51 |
| SP | 35 |
| GO | 52 |
| MS | 50 |
| RS | 43 |
| PR | 41 |
| SC | 42 |
| MG | 31 |

#### Adaptação por Modelo
- **NFE:** `xmlns="http://www.portalfiscal.inf.br/nfe"`
- **CTE:** `xmlns="http://www.portalfiscal.inf.br/cte"`, tag raiz: `<consStatServCTe>`
- **MDFE:** `xmlns="http://www.portalfiscal.inf.br/mdfe"`, tag raiz: `<consStatServMDFe>`

#### Validações
1. ❌ `ambiente` não pode ser nulo ou diferente de "PROD"/"HOMOL"
2. ❌ `uf` deve ter mapeamento no dicionário de códigos IBGE

---

### 4.2. ConsSitNFeBuilder (CONSULTA)

#### Campos Obrigatórios no Payload
- `chNFe` (String): Chave de acesso (44 dígitos)
- `ambiente` (String): "PROD" ou "HOMOL"

#### Estrutura XML Esperada
```xml
<?xml version="1.0" encoding="UTF-8"?>
<consSitNFe xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
    <tpAmb>1</tpAmb>
    <xServ>CONSULTAR</xServ>
    <chNFe>35210800000000000055550000000001001000000015</chNFe>
</consSitNFe>
```

#### Validações
1. ❌ `chNFe` deve ter exatamente 44 caracteres numéricos
2. ❌ `chNFe` deve passar validação de dígito verificador (Módulo 11)
3. ✅ Opcional: validar se UF da chave (posições 0-1) corresponde à UF do emitente

---

### 4.3. ConsultaCadastroBuilder (CONSULTA_CADASTRO)

#### Campos Obrigatórios no Payload
- `documento` (String): CPF (11 dígitos) ou CNPJ (14 dígitos)
- `uf` (String): UF do contribuinte a ser consultado

#### Estrutura XML Esperada
```xml
<?xml version="1.0" encoding="UTF-8"?>
<ConsCad xmlns="http://www.portalfiscal.inf.br/nfe" versao="2.00">
    <infCons>
        <xServ>CONS-CAD</xServ>
        <UF>MT</UF>
        <CNPJ>12345678000190</CNPJ>
    </infCons>
</ConsCad>
```

**OU (para CPF):**
```xml
<ConsCad xmlns="http://www.portalfiscal.inf.br/nfe" versao="2.00">
    <infCons>
        <xServ>CONS-CAD</xServ>
        <UF>MT</UF>
        <CPF>12345678901</CPF>
    </infCons>
</ConsCad>
```

#### Validações
1. ❌ `documento` deve ter 11 (CPF) ou 14 (CNPJ) dígitos
2. ✅ Escolher tag `<CPF>` ou `<CNPJ>` baseado no comprimento

---

### 4.4. InutNFeBuilder (INUTILIZACAO)

#### Campos Obrigatórios no Payload
- `ano` (String): Ano com 2 dígitos (ex: "26" para 2026)
- `serie` (String): Série da NF-e (ex: "1")
- `numIni` (String): Número inicial da faixa
- `numFin` (String): Número final da faixa
- `justificativa` (String): Motivo (mínimo 15 caracteres)
- `ambiente` (String): "PROD" ou "HOMOL"

#### Estrutura XML Esperada
```xml
<?xml version="1.0" encoding="UTF-8"?>
<inutNFe xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
    <infInut Id="ID51123456780001900155001000000010000000100">
        <tpAmb>1</tpAmb>
        <xServ>INUTILIZAR</xServ>
        <cUF>51</cUF>
        <ano>26</ano>
        <CNPJ>12345678000190</CNPJ>
        <mod>55</mod>
        <serie>1</serie>
        <nNFIni>10</nNFIni>
        <nNFFin>100</nNFFin>
        <xJust>Série descontinuada por mudança de sistema fiscal</xJust>
    </infInut>
</inutNFe>
```

#### Geração do Atributo `Id`
Formato: `ID{cUF}{CNPJ}{mod}{serie}{nNFIni}{nNFFin}`
- Paddings: `cUF` (2 dígitos), `serie` (3 dígitos), `nNFIni` (9 dígitos), `nNFFin` (9 dígitos)
- Exemplo: `ID51123456780001900155001000000010000000100`

#### Validações
1. ❌ `numIni` ≤ `numFin`
2. ❌ `justificativa` deve ter pelo menos 15 caracteres
3. ❌ `ano` deve ter 2 dígitos

---

### 4.5. EventoBuilder (EVENTO)

#### Campos Obrigatórios no Payload
- `chNFe` (String): Chave de acesso (44 dígitos)
- `tpEvento` (String): Código do evento (ex: "110111" = Cancelamento)
- `nSeqEvento` (String): Sequência do evento (geralmente "1")
- `nProt` (String): Protocolo de autorização da NF-e
- `justificativa` (String): Justificativa (para cancelamento: mínimo 15 caracteres)
- `ambiente` (String): "PROD" ou "HOMOL"

#### Estrutura XML Esperada (Cancelamento)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<envEvento xmlns="http://www.portalfiscal.inf.br/nfe" versao="1.00">
    <idLote>1</idLote>
    <evento versao="1.00">
        <infEvento Id="ID110111352108000000000000555500000000010010000000151">
            <cOrgao>35</cOrgao>
            <tpAmb>1</tpAmb>
            <CNPJ>12345678000190</CNPJ>
            <chNFe>35210800000000000055550000000001001000000015</chNFe>
            <dhEvento>2026-02-11T07:00:00-04:00</dhEvento>
            <tpEvento>110111</tpEvento>
            <nSeqEvento>1</nSeqEvento>
            <verEvento>1.00</verEvento>
            <detEvento versao="1.00">
                <descEvento>Cancelamento</descEvento>
                <nProt>351260000000123</nProt>
                <xJust>Pedido cancelado pelo cliente antes da entrega</xJust>
            </detEvento>
        </infEvento>
    </evento>
</envEvento>
```

#### Tipos de Evento Suportados
| Código | Descrição | Validações Específicas |
|--------|-----------|------------------------|
| 110111 | Cancelamento | `justificativa` ≥ 15 caracteres, `nProt` obrigatório |
| 110110 | Carta de Correção | `justificativa` ≥ 15 caracteres (texto da correção) |
| 210200 | Confirmação da Operação | Nenhuma justificativa necessária |
| 210210 | Ciência da Operação | Nenhuma justificativa necessária |

#### Geração de Campos Dinâmicos
- `dhEvento`: Data/hora atual no formato ISO 8601 com fuso horário
- `cOrgao`: Extraído dos 2 primeiros dígitos da `chNFe`
- `Id`: Formato `ID{tpEvento}{chNFe}{nSeqEvento}` (paddings: `nSeqEvento` = 2 dígitos)

#### Validações
1. ❌ `tpEvento` deve ser um dos códigos conhecidos
2. ❌ Para eventos 110111/110110: `justificativa` ≥ 15 caracteres
3. ❌ `chNFe` deve ter 44 dígitos válidos

---

## 5. Regras Transversais (Todos os Builders)

### 5.1. Tratamento de Erros

```java
if (campo == null || campo.isEmpty()) {
    throw new IllegalArgumentException(
        String.format("[%s] Campo obrigatório ausente: %s", 
        getClass().getSimpleName(), "nomeDoCampo")
    );
}
```

### 5.2. Logging

```java
System.out.println(String.format("[%s] XML gerado com sucesso para %s", 
    getClass().getSimpleName(), cnpj));
```

### 5.3. Encoding

Sempre usar `UTF-8` na declaração XML:
```xml
<?xml version="1.0" encoding="UTF-8"?>
```

### 5.4. Indentação

XML **NÃO** deve ter indentação (linha única) para economia de bytes na transmissão.

---

## 6. Checklist de Implementação

Para cada builder, seguir:

- [ ] Criar classe em `src/main/java/tech/vcinf/fiscalwebsocket/builder/{NomeBuilder}.java`
- [ ] Adicionar anotação `@Component`
- [ ] Implementar `XmlBuilder`
- [ ] Implementar método `build(...)` com validações
- [ ] Adicionar mapeamento em `XmlBuilderConfig.xmlBuilders()`
- [ ] Testar com payload de exemplo via `teste.html`
- [ ] Validar XML gerado contra XSD oficial (opcional, mas recomendado)

---

## 7. Exemplo Completo: ConsStatServBuilder

```java
package tech.vcinf.fiscalwebsocket.builder;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ConsStatServBuilder implements XmlBuilder {

    // Mapeamento UF → Código IBGE
    private static final Map<String, String> UF_CODES = Map.of(
        "MT", "51",  // Mato Grosso
        "SP", "35",  // São Paulo
        "GO", "52",  // Goiás
        "MS", "50",  // Mato Grosso do Sul
        "RS", "43",  // Rio Grande do Sul
        "PR", "41",  // Paraná
        "SC", "42",  // Santa Catarina
        "MG", "31"   // Minas Gerais
        // NOTA: Map.of() suporta no máximo 10 pares chave-valor.
        // Para adicionar mais UFs, use HashMap em bloco static {}
    );

    @Override
    public String build(Map<String, Object> payload, String cnpj, String uf) {
        // Validações
        String ambiente = (String) payload.get("ambiente");
        if (ambiente == null || (!ambiente.equals("PROD") && !ambiente.equals("HOMOL"))) {
            throw new IllegalArgumentException("[ConsStatServBuilder] Campo 'ambiente' deve ser 'PROD' ou 'HOMOL'");
        }

        String cUF = UF_CODES.get(uf);
        if (cUF == null) {
            throw new IllegalArgumentException("[ConsStatServBuilder] UF não mapeada: " + uf);
        }

        int tpAmb = ambiente.equals("PROD") ? 1 : 2;

        // Construção XML
        String xml = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<consStatServ xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">" +
            "<tpAmb>%d</tpAmb><cUF>%s</cUF><xServ>STATUS</xServ>" +
            "</consStatServ>",
            tpAmb, cUF
        );

        System.out.println("[ConsStatServBuilder] XML gerado para " + cnpj);
        return xml;
    }
}
```

---

## 8. Próximos Passos

1. **Criar `XmlBuilderConfig.java`** com bean `xmlBuilders`
2. **Implementar `ConsStatServBuilder`** (mais simples)
3. **Testar via `teste.html`** (enviar `action: "transmit"` sem campo `xml`)
4. **Implementar builders restantes** na ordem de complexidade
5. **Validar XMLs** contra XSDs oficiais da SEFAZ (opcional)

---

## 9. Política de Qualidade

- ✅ Código deve compilar sem warnings
- ✅ Validações devem ter mensagens de erro claras e específicas
- ✅ XMLs devem ser testados contra ambiente de homologação da SEFAZ
- ✅ Nenhum hardcoding de CNPJs ou chaves de teste no código

---

**Documento mantido por:** Victor Campos (vcinf.tech)  
**Última atualização:** 2026-02-11  
**Status:** 🟢 Pronto para implementação
