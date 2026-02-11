package tech.vcinf.fiscalwebsocket.service;

import org.springframework.stereotype.Service;
import tech.vcinf.fiscalwebsocket.dto.*;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OperationDiscoveryService {

    // Cache para as URLs do sefaz-urls.ini: <Modelo, <UF, <Ambiente, <Servico, URL>>>>
    private final Map<String, Map<String, Map<String, Map<String, String>>>> operationsCache = new HashMap<>();

    // Definição centralizada dos metadados de cada operação fiscal
    private static final Map<String, OperationMetadata> OPERATION_METADATA = new HashMap<>();

    // Record para armazenar os metadados de forma concisa
    private record OperationMetadata(String key, String label, String description, String category, boolean requiresSignature, List<FormField> fields) {}

    static {
        // Categoria: Consultas
        OPERATION_METADATA.put("NFeStatusServico4", new OperationMetadata("STATUS", "Status do Serviço", "Verifica a disponibilidade do web service da SEFAZ.", "Consultas", false, List.of()));
        OPERATION_METADATA.put("NFeConsulta4", new OperationMetadata("CONSULTA", "Consultar NF-e", "Consulta a situação de uma NF-e pela chave de acesso.", "Consultas", false, 
            List.of(newFormField("chNFe", "Chave de Acesso", "text", "Digite os 44 dígitos da chave de acesso", true))
        ));
        OPERATION_METADATA.put("CadConsultaCadastro4", new OperationMetadata("CONSULTA_CADASTRO", "Consulta Cadastro", "Consulta a situação cadastral de um contribuinte.", "Consultas", false, 
             List.of(newFormField("CNPJ", "CNPJ do Contribuinte", "text", "Digite o CNPJ a ser consultado", true))
        ));

        // Categoria: Transações
        OPERATION_METADATA.put("NFeAutorizacao4", new OperationMetadata("AUTORIZACAO", "Autorização", "Envia um lote de NF-es para autorização de uso.", "Transações", true, List.of())); // XML completo vem do cliente
        OPERATION_METADATA.put("NFeInutilizacao4", new OperationMetadata("INUTILIZACAO", "Inutilizar Numeração", "Solicita a inutilização de uma faixa de números de NF-e.", "Transações", true, 
            List.of(
                newFormField("serie", "Série", "number", "Série da NF-e", true),
                newFormField("numIni", "Número Inicial", "number", "Primeiro número da faixa", true),
                newFormField("numFin", "Número Final", "number", "Último número da faixa", true),
                newFormField("justificativa", "Justificativa", "textarea", "Motivo da inutilização (mín. 15 caracteres)", true)
            )
        ));

        // Categoria: Eventos
        OPERATION_METADATA.put("RecepcaoEvento4", new OperationMetadata("EVENTO", "Evento (Cancelamento/CC-e)", "Registra um evento para uma NF-e, como cancelamento ou carta de correção.", "Eventos", true, 
            List.of(
                newFormField("chNFe", "Chave de Acesso", "text", "Chave da NF-e a ser afetada", true),
                newFormField("tpEvento", "Tipo do Evento", "select", "", true), // Options preenchidas no front
                newFormField("nProt", "Protocolo", "text", "Protocolo de autorização da NF-e", true),
                newFormField("justificativa", "Justificativa / Correção", "textarea", "Justificativa do evento", true)
            )
        ));

         // Adicionar metadados para outros modelos (NFCe, CTe, MDFe) aqui...
    }

    // Helper para criar FormField de forma mais limpa
    private static FormField newFormField(String name, String label, String type, String placeholder, boolean required) {
        FormField field = new FormField();
        field.setName(name);
        field.setLabel(label);
        field.setType(type);
        field.setPlaceholder(placeholder);
        field.setRequired(required);
        return field;
    }

    @PostConstruct
    public void init() {
        // Carrega o arquivo de URLs para o cache na inicialização
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/sefaz-urls.ini")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;

                String[] parts = line.split("=");
                String key = parts[0];
                String url = parts[1];

                // Formato esperado: {MODELO}.{SERVICO}.{UF}.{AMBIENTE}
                String[] keyParts = key.split("\\.");
                if (keyParts.length == 4) {
                    String modelo = keyParts[0];
                    String servico = keyParts[1];
                    String uf = keyParts[2];
                    String ambiente = keyParts[3];

                    operationsCache
                        .computeIfAbsent(modelo, k -> new HashMap<>())
                        .computeIfAbsent(uf, k -> new HashMap<>())
                        .computeIfAbsent(ambiente, k -> new HashMap<>())
                        .put(servico, url);
                }
            }
        } catch (Exception e) {
            System.err.println("FALHA CRÍTICA: Não foi possível carregar o arquivo 'sefaz-urls.ini'. O serviço de operações não funcionará.");
            e.printStackTrace();
        }
    }

    public OperationCatalog getAvailableOperations(String modelo, String uf, String ambiente) {
        OperationCatalog catalog = new OperationCatalog();
        catalog.setModelo(modelo);
        catalog.setUf(uf);
        catalog.setAmbiente(ambiente);

        // Obtém os serviços disponíveis para a combinação de modelo, UF e ambiente
        Map<String, String> availableServices = operationsCache
            .getOrDefault(modelo, Collections.emptyMap())
            .getOrDefault(uf, Collections.emptyMap())
            .getOrDefault(ambiente, Collections.emptyMap());

        if (availableServices.isEmpty()) {
            catalog.setCategorias(Collections.emptyList());
            return catalog; // Retorna catálogo vazio se nada for encontrado
        }

        // Agrupa as operações disponíveis por sua categoria definida nos metadados
        Map<String, List<Operation>> groupedByCategory = availableServices.keySet().stream()
            .filter(OPERATION_METADATA::containsKey) // Processa apenas serviços com metadados definidos
            .map(serviceKey -> {
                OperationMetadata meta = OPERATION_METADATA.get(serviceKey);
                Operation op = new Operation();
                op.setChave(meta.key());
                op.setLabel(meta.label());
                op.setDescricao(meta.description());
                op.setRequerAssinatura(meta.requiresSignature());
                op.setCamposFormulario(meta.fields());
                return new AbstractMap.SimpleEntry<>(meta.category(), op); // Par (Categoria, Operação)
            })
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        // Converte o mapa agrupado para a lista de OperationCategory do DTO
        List<OperationCategory> categories = groupedByCategory.entrySet().stream()
            .map(entry -> {
                OperationCategory category = new OperationCategory();
                category.setNome(entry.getKey());
                category.setOperacoes(entry.getValue());
                // O ícone pode ser definido aqui com base no nome da categoria, se desejado
                if ("Consultas".equals(entry.getKey())) category.setIcone("🔍");
                if ("Transações".equals(entry.getKey())) category.setIcone("💼");
                if ("Eventos".equals(entry.getKey())) category.setIcone("✍️");
                return category;
            })
            .sorted(Comparator.comparing(OperationCategory::getNome)) // Ordena as categorias
            .collect(Collectors.toList());

        catalog.setCategorias(categories);
        return catalog;
    }
}
