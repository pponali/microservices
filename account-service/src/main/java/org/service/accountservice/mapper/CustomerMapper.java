package org.service.accountservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.service.accountservice.dto.AccountsDto;
import org.service.accountservice.dto.CustomerDto;
import org.service.accountservice.model.Account;
import org.service.accountservice.model.Customer;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Mapper
public interface CustomerMapper {
    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);
    Customer toEntity(CustomerDto customerDto);
    CustomerDto toDto(Customer customer);
}
