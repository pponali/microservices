package org.service.accountservice.service;

import org.service.accountservice.dto.CustomerDto;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */
public interface IAccountsService {

    /**
     *THis method is responsible fore create an account
     * @param customerDto - customerDto
     */
    void createAccount(CustomerDto customerDto);

    boolean updateAccount(CustomerDto customerDto);

    boolean deleteAccount(String mobileNumber);

    CustomerDto fetchAccount(String mobileNumber);

}
