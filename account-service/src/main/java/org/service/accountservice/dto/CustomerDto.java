package org.service.accountservice.dto;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.service.accountservice.model.BaseEntity;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Data @AllArgsConstructor @NoArgsConstructor
public class CustomerDto {
    Long customerId;

    @NotEmpty(message = "Name should not be empty")
    @Size(min = 5, max = 30 , message = "Name should be min of 5 and max of 30")
    String name;

    @Email
    String email;

    @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number should not contain the special chars.")
    String mobileNUmber;

    AccountsDto accountsDto;

}
