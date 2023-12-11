package org.service.accountservice.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */


@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode
@ToString
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", parameters = @org.hibernate.annotations.Parameter(name = "sequence", value = "SEQ_ID"))
    @Column(updatable = false, nullable = false, unique = true, length = 10, columnDefinition = "bigint")
    @Id
    Long id;

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
