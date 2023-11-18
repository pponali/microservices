package com.services.authorization.service;

import com.services.authorization.entity.Authority;
import com.services.authorization.entity.SecurityUser;
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

    @Override
    public void createUser(final UserDetails user) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setUsername(user.getUsername());
        securityUser.setPassword(user.getPassword());
        Set<Authority> authorities = new HashSet<>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            Authority userAuthorities = new Authority();
            userAuthorities.setAuthority(authority.getAuthority());
            authorities.add(userAuthorities);
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
    public boolean userExists(final String username) {
        SecurityUser securityUser =  userRepository.findByUsername(username);
        return securityUser != null;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        SecurityUser securityUser = userRepository.findByUsername(username);
        if(securityUser == null){
            throw new UsernameNotFoundException("User Access denied");
        }
        Collection<GrantedAuthority> authorities = new HashSet<>();
        securityUser.getAuthorities().forEach(authority -> {
            authorities.add(new SimpleGrantedAuthority(authority.getAuthority()));
        });

        return new User(securityUser.getUsername(),
                securityUser.getPassword(),
                securityUser.getEnabled(),
                securityUser.getAccountNonExpired(),
                securityUser.getCredentialsNonExpired(),
                securityUser.getAccountNonLocked(),
                authorities);

    }
}
