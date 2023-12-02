package com.wlh.smartbi.service;


import com.wlh.smartbi.model.enums.MailEnum;

/**
 * @author WLH
 * @interface MessageService
 * @date : 2023/08/26/ 12:22
 **/
public interface MessageService {

    /**
     * 发送验证码
     *
     * @param receiver 接收机
     * @param mailEnum 邮件枚举
     * @return boolean
     */
    boolean sendCode(String receiver, String code, MailEnum mailEnum);
}
