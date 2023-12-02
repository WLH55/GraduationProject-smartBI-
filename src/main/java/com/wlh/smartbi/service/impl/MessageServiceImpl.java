package com.wlh.smartbi.service.impl;

import cn.hutool.core.date.DateUtil;

import com.wlh.smartbi.config.MailConfig;
import com.wlh.smartbi.model.enums.MailEnum;
import com.wlh.smartbi.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author WLH
 * @className MessageServiceImpl
 * @date : 2023/08/26/ 12:38
 **/
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    MailConfig mailConfig;

    @Override
    public boolean sendCode(String receiver, String code, MailEnum mailEnum) {
        // 拼接内容
        String content = mailEnum.getPrefix() + code + mailEnum.getSuffix();
        // 设置信息
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailConfig.getUsername());
        message.setTo(receiver);
        message.setSubject(mailEnum.getTitle());
        message.setText(content);
        // 发送邮件
        javaMailSender.send(message);
        log.info("邮件发送成功！to：【{}】 -- 验证码：【{}】 -- {}", receiver, code, DateUtil.now());
        return true;
    }

}
