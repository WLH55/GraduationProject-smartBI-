package com.wlh.smartbi.model.DTO.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Date;

/**
 * @author WLH
 * @className UserUpdateRequest
 * @date : 2023/08/26/ 23:11
 **/
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * 昵称
     */
    private String userName;


    /**
     * 地址
     */
    private String address;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 性别1男0女
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;
    /**
     * 是否是管理员
     */
    private String userRole;

    /**
     * 出生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Nullable
    private Date birth;

    private static final long serialVersionUID = 1L;
}
