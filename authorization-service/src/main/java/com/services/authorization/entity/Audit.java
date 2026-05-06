package com.services.authorization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@MappedSuperclass
@Getter @Setter @EqualsAndHashCode @ToString
public class Audit {


    @CreatedBy
    @Column(updatable = false)
    String createdBy;

    @LastModifiedBy
    @Column(updatable = false)
    String lastModifiedBy;

    @LastModifiedDate
    @Column(updatable = false)
    LocalDateTime lastModifiedDate;

    @CreatedDate
    @Column(updatable = false)
    LocalDateTime createdDate;

    @Version
    @Column(updatable = false)
    Long version;

}
