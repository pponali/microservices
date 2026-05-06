package com.services.authorization.service;

import com.services.authorization.entity.Authority;
import com.services.authorization.entity.SecurityUser;
import com.services.authorization.repository.JpaAuthorityRepository;
import com.services.authorization.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author prakashponali
 * @Date 26/10/23
 */

@Service
@RequiredArgsConstructor
public class JpaUserDetailsManager implements UserDetailsManager {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JpaAuthorityRepository jpaAuthorityRepository;

    @Override
    public void createUser(final UserDetails user) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setUsername(user.getUsername());
        securityUser.setPassword(user.getPassword());
        Set<Authority> authorities = new HashSet<>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            if(jpaAuthorityRepository.findByAuthority(authority.getAuthority()).isPresent()){
                authorities.add(jpaAuthorityRepository.findByAuthority(authority.getAuthority()).get());
            }
        }
        securityUser.setAuthorities(authorities);
        userRepository.save(securityUser);

    }

    @Override
    public void updateUser(final UserDetails user) {

    }

    @Override
    public void deleteUser(final String username) {

    }

    @Override
    public void changePassword(final String oldPassword, final String newPassword) {

    }

    @Override
    public boolean userExists(final String username) throws UsernameNotFoundException {
        Optional<SecurityUser> securityUser =  userRepository.findByUsername(username);
        if(securityUser.isPresent())
        {
            return true;
        } else {
            throw new UsernameNotFoundException("User " + username + " not found");
        }
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        Optional<SecurityUser> securityUser = userRepository.findByUsername(username);

        if(securityUser.isEmpty()){
            throw new UsernameNotFoundException("User Access denied");
        }
        SecurityUser user =  securityUser.get();
        Collection<GrantedAuthority> authorities = new HashSet<>();
        user.getAuthorities().forEach(authority -> {
            authorities.add(new SimpleGrantedAuthority(authority.getAuthority()));
        });

        return new User(user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                user.getAccountNonExpired(),
                user.getCredentialsNonExpired(),
                user.getAccountNonLocked(),
                authorities);

    }
}
