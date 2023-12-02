package com.wlh.smartbi.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wlh.smartbi.model.DO.ChartLogEntity;
import com.wlh.smartbi.model.DTO.ChartLogDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author  wlh
* @description 针对表【chart_logs】的数据库操作Mapper
* @createDate 2023-09-03 13:24:19
* @Entity com. wlh.bi.model.DO.ChartLogEntity
*/
public interface ChartLogMapper extends BaseMapper<ChartLogEntity> {

    List<ChartLogDTO> getLogs(@Param("dayCount") Integer dayCount, @Param("userId")Long userId);
}




