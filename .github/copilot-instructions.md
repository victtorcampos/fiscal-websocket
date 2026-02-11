<!-- Copilot / AI agent instructions for tech.vcinf:fiscal-websocket -->
# Quick onboarding for AI coding agents

- **Purpose:** middleware that bridges a WebSocket/STOMP front-end and SEFAZ web services (DF-e) using mTLS and XML signing.
- **Primary language / framework:** Java 17, Spring Boot, Apache Commons HttpClient.

## Big picture (what to know first)

- The app exposes a STOMP endpoint `/ws` and accepts client requests at application destination `/app/transmitir`. Responses are published to `/topic/responses`.
- `tech.vcinf.fiscalwebsocket.controller.FiscalController` is the message entrypoint: it interprets an `action` field (examples: `register`, `get_operations`, generic `transmit`) and orchestrates services.
- XML construction is delegated by service-key to implementations of `tech.vcinf.fiscalwebsocket.builder.XmlBuilder` (in `builder/`). For some operations the client provides full XML (e.g. `AUTORIZACAO`).
- Certificate & mTLS: `SefazProtocolFactory` builds an Apache `Protocol` that uses `SocketFactoryDinamico` and a per-emitente KeyStore; the cacert bundle lives in `src/main/resources/cacert`.
- URL discovery: `sefaz-urls.ini` (resource) is parsed by `OperationDiscoveryService` and `UfWebService` to map `{MODEL}.{SERVICE}.{UF}.{AMBIENTE}` → endpoint URL.
- Outbound calls to SEFAZ are done with `SefazService.send(...)`, which wraps XML in a SOAP envelope via `SoapEnvelopeUtils` and uses Apache Commons HttpClient configured with the custom Protocol.

## Key files to reference (examples)

- Message handling: `src/main/java/tech/vcinf/fiscalwebsocket/controller/FiscalController.java`
- WebSocket config: `src/main/java/tech/vcinf/fiscalwebsocket/config/WebSocketConfig.java`
- mTLS / Protocol: `src/main/java/tech/vcinf/fiscalwebsocket/service/SefazProtocolFactory.java`
- HTTP/soap sender: `src/main/java/tech/vcinf/fiscalwebsocket/service/SefazService.java`
- URL mapping: `src/main/resources/sefaz-urls.ini` and `src/main/java/tech/vcinf/fiscalwebsocket/service/OperationDiscoveryService.java`
- XML builders: `src/main/java/tech/vcinf/fiscalwebsocket/builder/` (implements `XmlBuilder`)
- Certificate helpers: `src/main/java/tech/vcinf/fiscalwebsocket/service/CertificateManager.java` and `util/CertificateUtils.java`

## Message contract & important conventions

- Client → server: send to `/app/transmitir` a JSON matching `FiscalRequest` (see `dto/FiscalRequest.java`). Common `action` values:
  - `register`: Base64 PFX + password. Controller writes `cert_{CNPJ}.pfx` in working dir and stores metadata in `Emitente`.
  - `get_operations`: returns `OperationCatalog` filled from `OperationDiscoveryService` metadata and `sefaz-urls.ini`.
  - `transmit` (generic): requires `cnpj`, `servico` (logical key like `STATUS`, `AUTORIZACAO`), `modelo`, `ambiente`, and either `xml` (client-provided) or builder-generated XML.
- Services that require signing are defined centrally (see `FiscalController.SERVICES_WITHOUT_SIGNATURE` and `OperationDiscoveryService` metadata). For signed services the controller creates a temp file, calls `XmlSignatureService.sign(...)`, and reads the signed XML back.
- Transaction logging: operations `AUTORIZACAO`, `INUTILIZACAO`, `EVENTO` are persisted via `TransactionLogRepository`.

## Environment, build & run

- The README contains basic run steps. Common commands:
  - development (Unix/macOS): `mvn spring-boot:run` or `./mvnw spring-boot:run`
  - Windows PowerShell: `mvn spring-boot:run` or `mvnw.cmd spring-boot:run`
- The app stores uploaded PFX files in the process working directory as `cert_{CNPJ}.pfx` — tests and local runs rely on that behavior.

## Project-specific patterns an agent should follow

- Prefer small, focused edits. Many components use injected beans (constructor injection). Preserve existing constructors and bean names when adding new services.
- When changing URL resolution or operation metadata, update `OperationDiscoveryService.OPERATION_METADATA` and `sefaz-urls.ini` together so front-end `get_operations` remains consistent.
- Changes to mTLS or protocol creation must respect the caching in `SefazProtocolFactory.protocolCache` to avoid reloading KeyStores on every request.
- Builders: add a new `XmlBuilder` implementation in `builder/` and ensure it is a Spring bean (component) so it is available in the `xmlBuilders` map injected into `FiscalController`.

## External integration notes

- SEFAZ endpoints: configured via `src/main/resources/sefaz-urls.ini` — do not hardcode URLs elsewhere.
- Truststore: the app expects `src/main/resources/cacert` (JKS) with password `changeit` (see code). Be careful modifying its format.

## When adding tests / running locally

- No test suite is present by default. For integration-like checks, run the app and use the example STOMP JS snippet from `README.md` to exercise messaging flows.

## Editing / PR guidance for AI agents

- Keep changes minimal and well-scoped: update metadata and builders together; avoid moving how certificates are persisted without updating `CertificateManager` and usages.
- When adding endpoints or services, add explicit references to the DTOs in `dto/` and confirm the STOMP message shape used by `FiscalController`.

---
Please review any unclear points (e.g., where certificates should be stored in production, or how to run with a real SEFAZ environment) and tell me which section to expand or adjust.
