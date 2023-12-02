package com.wlh.smartbi.model.DTO.chart;

import lombok.Data;

/**
 * @author WLH
 * @className BiResponse
 * @date : 2023/07/07/ 18:17
 **/
@Data
public class BiResponse {

    /**
     * 生成图表的代码
     */
    private String genChart;

    /**
     * 生成结果
     */
    private String genResult;

    /**
     * 表id
     */
    private Long chartId;


}
