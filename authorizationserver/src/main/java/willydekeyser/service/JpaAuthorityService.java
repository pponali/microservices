package willydekeyser.service;

import org.springframework.stereotype.Service;
import willydekeyser.entity.Authority;
import willydekeyser.repository.JpaAuthorityRepository;

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
        return authority.isEmpty();

    }

}
