package com.wlh.smartbi.service;


import com.wlh.smartbi.model.DO.ChartEntity;
import com.wlh.smartbi.model.DTO.chart.BiResponse;

/**
 * @author WLH
 * @className GenChartStrategy
 * @date : 2023/08/30/ 11:41
 **/
public interface GenChartStrategy {

    /**
     * 执行图表生成
     *
     * @param chartEntity 表实体
     * @return {@link BiResponse}
     */
    BiResponse executeGenChart(ChartEntity chartEntity);
}
