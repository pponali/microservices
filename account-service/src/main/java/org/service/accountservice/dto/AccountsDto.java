package org.service.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Random;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Data @AllArgsConstructor
public class AccountsDto implements Serializable {

    String accountId = "1000" + new Random().nextInt();

    String branchAddress;

    String accountNumber;

    String accountType;

}
