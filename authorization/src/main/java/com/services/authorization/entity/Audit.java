package com.services.authorization.entity;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@MappedSuperclass
public class Audit {
    @Id
    Long id;

    @LastModifiedDate
    LocalDate lastModifiedDate;

    @CreatedDate
    LocalDate createdDate;

    @Version
    Long version;

}
