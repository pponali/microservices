package org.service.accountservice.repository;

import org.service.accountservice.model.Account;
import org.service.accountservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */
public interface AccountsRepository extends JpaRepository<Account, Long> {
    Account findByAccountNumber(Long accountNumber);
    Optional<Account> findByCustomerId(Long customerId);
}
