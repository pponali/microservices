package com.services.authorization.service;

import com.services.authorization.entity.Authority;
import com.services.authorization.repository.JpaAuthorityRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author prakashponali
 * @Date 26/10/23
 */

@Service
public class JpaAuthorityService {


    private final JpaAuthorityRepository jpaAuthorityRepository;

    public JpaAuthorityService(final JpaAuthorityRepository jpaAuthorityRepository) {
        this.jpaAuthorityRepository = jpaAuthorityRepository;
    }

    public void createAuthority(final Authority authority) {
        jpaAuthorityRepository.save(authority);
    }

    public boolean authorityExists(final String username) {
        Optional<Authority> authority =  jpaAuthorityRepository.findByAuthority(username);
        if(authority.isPresent()){
            return true;
        }
        return false;
    }

}
