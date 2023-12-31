package com.services.authorization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@Entity
@Table(name="address")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Address extends Audit{
    private String city;
    private String state;
}
