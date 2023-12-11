package com.services.authorization.entity;

import com.services.authorization.annotations.ValidEmail;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author prakashponali
 * @Date 26/10/23
 */


@Entity
@Table(name="users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecurityUser {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String username;

    @Column
    private String password;


    @JoinTable(name = "users_authorities", joinColumns = {
            @JoinColumn(name = "USERS_ID", referencedColumnName = "ID")}, inverseJoinColumns = {
            @JoinColumn(name = "AUTHORITIES_ID", referencedColumnName = "ID")
    })
    @ManyToMany(cascade = {CascadeType.MERGE} , fetch = FetchType.EAGER)
    private Set<Authority> authorities;

    private Boolean accountNonExpired = Boolean.TRUE;
    private Boolean accountNonLocked = Boolean.TRUE;
    private Boolean enabled = Boolean.TRUE;
    private Boolean credentialsNonExpired  = Boolean.TRUE;
    //@ValidEmail
    private String email;

}
