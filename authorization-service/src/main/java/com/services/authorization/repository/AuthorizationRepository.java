package com.services.authorization.repository;

import com.services.authorization.entity.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author prakashponali
 * @Date 26/10/23
 */


import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author prakashponali
 * @Date 05/10/23
 */
public interface AuthorizationRepository extends JpaRepository<Authorization, String> {

    Optional<Authorization> findByState(String state);
    Optional<Authorization> findByAuthorizationCodeValue(String authorizationCode);
    Optional<Authorization> findByAccessTokenValue(String accessToken);
    Optional<Authorization> findByRefreshTokenValue(String refreshToken);
    @Query("select a from Authorization a where a.state = :token" +
            " or a.authorizationCodeValue = :token" +
            " or a.accessTokenValue = :token" +
            " or a.refreshTokenValue = :token"
    )
    Optional<Authorization> findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValue(@Param("token") String token);
}