package com.wlh.smartbi.service.execution;


import com.wlh.smartbi.common.ErrorCode;
import com.wlh.smartbi.common.exception.BusinessException;
import com.wlh.smartbi.model.DO.ChartEntity;
import com.wlh.smartbi.model.DTO.chart.BiResponse;
import com.wlh.smartbi.service.GenChartStrategy;
import org.springframework.stereotype.Component;

/**
 * 拒绝策略
 *
 * @author WLH
 * @className GenChartSync
 * @date 2023/08/30
 */
@Component(value = "gen_reject")
public class GenChartReject implements GenChartStrategy {

    @Override
    public BiResponse executeGenChart(ChartEntity chartEntity) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "服务器繁忙,请稍后重试!");
    }
}
