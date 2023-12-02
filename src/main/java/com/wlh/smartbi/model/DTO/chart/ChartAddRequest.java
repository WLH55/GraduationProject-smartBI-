package com.wlh.smartbi.model.DTO.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * @author WLH
 * @className ChartAddRequest
 * @date : 2023/07/04/ 10:21
 **/
@Data
public class ChartAddRequest implements Serializable {

    /**
     * 名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * wait,running,succeed,failed
     */
    private String status;

    /**
     * 执行信息
     */
    private String execMessage;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}
