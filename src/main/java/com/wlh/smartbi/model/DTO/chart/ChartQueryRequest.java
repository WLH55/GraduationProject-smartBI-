package com.wlh.smartbi.model.DTO.chart;

import com.wlh.smartbi.model.DTO.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author WLH
 * @className ChartEntityQueryRequest
 * @date : 2023/07/04/ 10:31
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * wait,running,succeed,failed
     */
    private String status;

    /**
     * 执行信息
     */
    private String execMessage;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;

}
