package org.service.accountservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.service.accountservice.constants.AccountsConstants;
import org.service.accountservice.dto.CustomerDto;
import org.service.accountservice.dto.ResponseDto;
import org.service.accountservice.service.IAccountsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */


@Validated
@RestController
@RequestMapping(path = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE}, headers = "Accept=application/json")
public class AccountsController {

    private IAccountsService accountsService;

    @PostMapping(value = "/create")
    public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody CustomerDto customerDto) {
        accountsService.createAccount(customerDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto(AccountsConstants.STATUS_201, AccountsConstants.STATUS_MESSAGE));


    }

    @GetMapping(value = "/fetch")
    public ResponseEntity<CustomerDto> fetchAccount(
            @Pattern(regexp = "(^$|[0-9]{10})",
                    message = "Mobile number should not contain the special chars.")
            String mobileNumber) {
        ;
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accountsService.fetchAccount(mobileNumber));
    }

    @GetMapping(value = "/fetch")
    public ResponseEntity<CustomerDto> update(@Valid @RequestBody CustomerDto customerDto) {
        boolean updated = accountsService.updateAccount(customerDto);
        if (updated) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(customerDto);

        } else {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body(customerDto);
        }
    }

    @GetMapping(value = "/fetch")
    public ResponseEntity<?> delete(
            @Pattern(regexp = "(^$|[0-9]{10})",
                    message = "Mobile number should not contain the special chars.")
            String mobileNumber) {
        boolean updated = accountsService.deleteAccount(mobileNumber);
        if (updated) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Optional.empty());

        } else {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .body(Optional.empty());
        }
    }
}
