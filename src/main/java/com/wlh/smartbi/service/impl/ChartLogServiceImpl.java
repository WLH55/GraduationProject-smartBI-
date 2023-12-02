package com.wlh.smartbi.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlh.smartbi.mapper.ChartLogMapper;
import com.wlh.smartbi.model.DO.ChartEntity;
import com.wlh.smartbi.model.DO.ChartLogEntity;
import com.wlh.smartbi.model.DTO.ChartLogDTO;
import com.wlh.smartbi.model.enums.ChartStatusEnum;
import com.wlh.smartbi.service.ChartLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dhx
 * @description 针对表【chart_logs】的数据库操作Service实现
 * @createDate 2023-09-03 13:24:19
 */
@Service
public class ChartLogServiceImpl extends ServiceImpl<ChartLogMapper, ChartLogEntity>
        implements ChartLogService {


    @Override
    public Long recordLog(ChartEntity chartEntity) {
        ChartLogEntity chartLogEntity = new ChartLogEntity();
        chartLogEntity.setUserId(chartEntity.getUserId());
        chartLogEntity.setChartId(chartEntity.getId());
        if (chartEntity.getStatus().equals(ChartStatusEnum.SUCCEED.getStatus())) {
            chartLogEntity.setResult(ChartStatusEnum.SUCCEED.getStatus());
        } else {
            chartLogEntity.setResult(ChartStatusEnum.FAILED.getStatus());
        }
        boolean save = save(chartLogEntity);
        return chartLogEntity.getLogId();
    }

    @Override
    public List<ChartLogDTO> getLogs(Integer dayCount, Long userId) {
        return  this.baseMapper.getLogs(dayCount,userId);
    }
}




