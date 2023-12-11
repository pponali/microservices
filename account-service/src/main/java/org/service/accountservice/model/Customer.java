package org.service.accountservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class Customer extends BaseEntity{

    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", parameters = @org.hibernate.annotations.Parameter(name = "sequence", value = "SEQ_ID"))
    @Column(updatable = false, nullable = false, unique = true, length = 10, columnDefinition = "bigint")
    @Id
    Long customerId;

    String name;

    String email;

    String mobileNumber;
}
