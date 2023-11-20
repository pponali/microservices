package com.services.authorization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author prakashponali
 * @Date 19/11/23
 * @Description
 */

@Entity
@Table(name="address")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Address extends Audit{
    private String city;
    private String state;
}
