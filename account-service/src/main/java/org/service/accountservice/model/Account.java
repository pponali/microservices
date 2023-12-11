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
    @Column(updatable = false, nullable = false, unique = true, length = 10, columnDefinition = "bigint")
    @Id
    Long accountId;

    @Column(updatable = false, nullable = false, unique = true, length = 10, columnDefinition = "bigint")
    @JoinColumn(name = "customerId", referencedColumnName = "customerId", nullable = false, updatable = false, unique = true, columnDefinition = "bigint")
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    Customer customerId;

    String branchAddress;

    String accountNumber;

    String accountType;



}
