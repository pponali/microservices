package org.service.accountservice.service.impl;

import lombok.AllArgsConstructor;
import org.service.accountservice.dto.AccountsDto;
import org.service.accountservice.dto.CustomerDto;
import org.service.accountservice.exception.CustomerAlreadyExistException;
import org.service.accountservice.exception.ResourceNotFountException;
import org.service.accountservice.mapper.AccountsMapper;
import org.service.accountservice.mapper.CustomerMapper;
import org.service.accountservice.model.Account;
import org.service.accountservice.model.Customer;
import org.service.accountservice.repository.AccountsRepository;
import org.service.accountservice.repository.CustomersRepository;
import org.service.accountservice.service.IAccountsService;
import org.springframework.stereotype.Service;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private AccountsRepository accountsRepository;

    private CustomersRepository customersRepository;
    @Override
    public void createAccount(CustomerDto customerDto) {
        Customer customer = CustomerMapper.INSTANCE.toEntity(customerDto);
        if(customersRepository.findByMobileNumber(customerDto.getMobileNUmber()).isPresent()){
            throw new CustomerAlreadyExistException("Customer already exist with mobile number " + customerDto.getMobileNUmber());
        }
        customersRepository.save(customer);
    }

    @Override
    public boolean updateAccount(CustomerDto customerDto) {

        return false;
    }

    @Override
    public boolean deleteAccount(String mobileNumber) {

        return false;
    }

    @Override
    public CustomerDto fetchAccount(String mobileNumber) {
        Customer customer = customersRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFountException("customer", "mobileNumber", mobileNumber)
        );
        CustomerDto customerDto = CustomerMapper.INSTANCE.toDto(customer);
        Account account = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFountException("customer", "mobileNumber", customerDto.getCustomerId().toString())
        );
        AccountsDto accountsDto = AccountsMapper.INSTANCE.toDto(account);
        customerDto.setAccountsDto(accountsDto);
        return customerDto;
    }
}
