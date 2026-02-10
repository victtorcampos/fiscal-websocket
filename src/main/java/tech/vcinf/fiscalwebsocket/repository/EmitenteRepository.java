package tech.vcinf.fiscalwebsocket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.vcinf.fiscalwebsocket.model.Emitente;

public interface EmitenteRepository extends JpaRepository<Emitente, String> {
}
