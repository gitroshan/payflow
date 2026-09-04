package com.payflow.gateway.repository;

import com.payflow.gateway.domain.GatewayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatewayTransactionRepository extends JpaRepository<GatewayTransaction, String> {
}
