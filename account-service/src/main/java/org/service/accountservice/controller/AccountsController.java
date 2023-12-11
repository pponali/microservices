package org.service.accountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.service.accountservice.constants.AccountsConstants;
import org.service.accountservice.dto.CustomerDto;
import org.service.accountservice.dto.ErrorResponseDto;
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


@Tag(name = "Accounts", description = "Accounts API")
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600, allowCredentials = "true")
@Validated
@RestController
@RequestMapping(path = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE}, headers = "Accept=application/json")
public class AccountsController {

    private IAccountsService accountsService;

    @Operation(summary = "Create Account", description = "Create Account", tags = {"Accounts"})
    @ApiResponse(responseCode = "201", description = "Response information after creating the account")
    @ResponseStatus(HttpStatus.CREATED)
    @NotNull(message = "Customer details cannot be null")
    @Validated
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

    @Operation(
            summary = "Update Account",
            description = "Update Account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Response information after updating the account"),
            @ApiResponse(
                    responseCode = "500",
                    description = "If the update is failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
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


    @Operation(
            summary = "Delete Account",
            description = "Delete Account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Response information after deleting the account"),
            @ApiResponse(responseCode = "500", description = "If the delete is failed")
    })
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
