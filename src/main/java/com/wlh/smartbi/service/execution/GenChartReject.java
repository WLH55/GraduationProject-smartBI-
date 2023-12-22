package com.wlh.smartbi.service.execution;


import com.wlh.smartbi.common.ErrorCode;
import com.wlh.smartbi.common.exception.BusinessException;
import com.wlh.smartbi.model.DO.ChartEntity;
import com.wlh.smartbi.model.DTO.chart.BiResponse;
import com.wlh.smartbi.model.enums.ChartStatusEnum;
import com.wlh.smartbi.service.ChartService;
import com.wlh.smartbi.service.GenChartStrategy;
import com.wlh.smartbi.utils.ThrowUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.Resource;

/**
 * 拒绝策略
 *
 * @author WLH
 * @className GenChartSync
 * @date 2023/08/30
 */
@Component(value = "gen_reject")
public class GenChartReject implements GenChartStrategy {
    @Resource
    private ChartService chartService;


    @Override
    public BiResponse executeGenChart(ChartEntity chartEntity) {
        chartEntity.setStatus(ChartStatusEnum.FAILED.getStatus());
        boolean save = chartService.save(chartEntity);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "保存图表失败!");
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "服务器繁忙,请稍后重试!");
    }
}
