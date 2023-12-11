package com.services.authorization.dto;

import com.services.authorization.annotations.ValidEmail;
import com.services.authorization.entity.Authority;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Data
public class SecurityUserDto implements Serializable {

    private String username;

    private String password;

    private Set<Authority> authorities;

    @ValidEmail
    private String email;
}
