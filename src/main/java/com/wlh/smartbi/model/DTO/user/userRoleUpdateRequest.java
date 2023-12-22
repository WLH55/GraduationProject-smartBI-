package com.wlh.smartbi.model.DTO.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description:
 * @Version: 1.0
 *
 */
@Data
public class userRoleUpdateRequest implements Serializable {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户角色
     */
    private String userRole;



    private static final long serialVersionUID = 1L;
}
