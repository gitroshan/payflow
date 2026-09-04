package com.payflow.ledger.repository;

import com.payflow.ledger.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByOwnerId(String ownerId);
}
