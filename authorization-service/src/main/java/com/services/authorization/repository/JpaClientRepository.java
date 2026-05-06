package com.services.authorization.repository;

import com.services.authorization.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;



/**
 * @author prakashponali
 * @Date 26/10/23
 */

@Repository
public interface JpaClientRepository extends JpaRepository<Client, String> {

    Optional<Client> findByClientId(final String clientId);
}
