package org.service.accountservice.repository;

import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.service.accountservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */
public interface CustomersRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByMobileNumber(String mobileNumber);


    @Transactional
    @Modifying
    void deleteById(@NotNull Long id);
}
