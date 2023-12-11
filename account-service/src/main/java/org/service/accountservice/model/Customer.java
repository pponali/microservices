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

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO,generator="native")
    @GenericGenerator(name = "native",strategy = "native")
    @Column(name="customer_id")
    Long customerId;

    String name;

    String email;
    @Column(name="mobile_number")
    String mobileNumber;
}
