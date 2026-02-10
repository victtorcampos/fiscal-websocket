package tech.vcinf.fiscalwebsocket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.vcinf.fiscalwebsocket.model.TransactionLog;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
}
