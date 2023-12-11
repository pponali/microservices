package org.service.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

@Data @AllArgsConstructor
@Schema(name = "Customer details", description = "Customer details")
@EqualsAndHashCode(callSuper = false, of = {"customerId", "name", "email", "mobileNUmber", "accountsDto"})

public class CustomerDto {
    Long customerId;

    @Schema(description = "Name of the customer", example = "Prakash Ponali")
    @Pattern(regexp = "^[a-zA-Z0-9 ]*$", message = "Name should not contain special chars.")
    @NotEmpty(message = "Name should not be empty")
    @Size(min = 5, max = 30 , message = "Name should be min of 5 and max of 30")
    String name;

    @Schema(description = "Email of the customer", example = "prakashponali@gmail.com")
    @NotEmpty(message = "Email should not be empty")
    @Email
    String email;

    @Schema(description = "Mobile number of the customer", example = "9999999999")
    @NotEmpty(message = "Mobile number should not be empty")
    @Size(min = 10, max = 10, message = "Mobile number should be 10 digits")
    @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number should not contain the special chars.")
    String mobileNUmber;

    AccountsDto accountsDto;

}
