package org.service.accountservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
public class Account extends BaseEntity{


    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", parameters = @org.hibernate.annotations.Parameter(name = "sequence", value = "SEQ_ID"))
    @Column(name="account_number", updatable = false, nullable = false, unique = true, length = 10, columnDefinition = "bigint")
    @Id
    Long accountNumber;


    @Column(name="customer_id")
    Long customerId;

    @Column(name="branch_address")
    String branchAddress;


    @Column(name="account_type")
    String accountType;



}
