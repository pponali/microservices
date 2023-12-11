package org.service.accountservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.service.accountservice.dto.AccountsDto;
import org.service.accountservice.model.Account;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Mapper
public interface AccountsMapper {

    AccountsMapper INSTANCE = Mappers.getMapper(AccountsMapper.class);
    Account toEntity(AccountsDto accountsDto);
    AccountsDto toDto(Account account);
}
