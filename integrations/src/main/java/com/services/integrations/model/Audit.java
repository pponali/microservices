package com.services.integrations.model;


import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;


@Data
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int Id;

    @LastModifiedDate
    Instant lastModifiedDate;

    @CreatedDate
    Instant createdDate;
}
