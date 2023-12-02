package com.wlh.smartbi.controller;


import com.wlh.smartbi.common.BaseResponse;
import com.wlh.smartbi.common.ErrorCode;
import com.wlh.smartbi.model.DTO.ChartLogDTO;
import com.wlh.smartbi.service.ChartLogService;
import com.wlh.smartbi.utils.ResultUtil;
import com.wlh.smartbi.utils.ThrowUtils;
import com.wlh.smartbi.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author WLH
 * @className ChartLogController
 * @date : 2023/09/03/ 13:54
 **/
@RestController
@Slf4j
public class ChartLogController {

    @Resource
    ChartLogService logService;


    @GetMapping("/chart/log")
    public BaseResponse<List<ChartLogDTO>> getLastDayLog(@RequestParam("count")Integer dayCount){
        ThrowUtils.throwIf(dayCount<0 || dayCount>90, ErrorCode.PARAMS_ERROR,"请求天数错误!");
        Long userId = UserHolder.getUser().getUserId();
        List<ChartLogDTO> logs=  logService.getLogs(dayCount,userId);
        return ResultUtil.success(logs);
    }

}
