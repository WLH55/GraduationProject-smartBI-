package com.wlh.smartbi.common.constant;

import lombok.Data;

/**
 * @author WLH
 * @verstion 1.0
 */
@Data
public class UserRole {
    //普通用户0，管理员1，封号2
    public static final String USER_ROLE_NORMAL = "0";
    public static final String USER_ROLE_ADMIN = "1";
    public static final String USER_ROLE_BAN = "2";
}
