package com.services.authorization.repository;

import com.services.authorization.entity.SecurityUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author prakashponali
 * @Date 26/10/23
 */
public interface UserRepository extends JpaRepository<SecurityUser, Integer> {

    Optional<SecurityUser> findByUsername(String userName);


}
