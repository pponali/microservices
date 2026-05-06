package com.services.authorization.repository;

import com.services.authorization.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author prakashponali
 * @Date 26/10/23
 */

@Repository
public interface JpaAuthorityRepository extends JpaRepository<Authority, Integer> {

    Optional<Authority> findByAuthority(String authority);


}
