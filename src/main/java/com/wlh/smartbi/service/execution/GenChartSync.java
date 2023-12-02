package com.wlh.smartbi.service.execution;


import com.wlh.smartbi.common.ErrorCode;
import com.wlh.smartbi.common.constant.AIConstant;
import com.wlh.smartbi.common.exception.BusinessException;
import com.wlh.smartbi.common.exception.GenChartException;
import com.wlh.smartbi.manager.AiManager;
import com.wlh.smartbi.model.DO.ChartEntity;
import com.wlh.smartbi.model.DTO.chart.BiResponse;
import com.wlh.smartbi.model.enums.ChartStatusEnum;
import com.wlh.smartbi.service.ChartLogService;
import com.wlh.smartbi.service.ChartService;
import com.wlh.smartbi.service.GenChartStrategy;
import com.wlh.smartbi.utils.ChartUtil;
import com.wlh.smartbi.utils.ThrowUtils;
import com.wlh.smartbi.webSocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * 同步生成
 *
 * @author WLH
 * @className GenChartSync
 * @date : 2023/08/30/ 11:46
 **/
@Component(value = "gen_sync")
@Slf4j
//@Component(value = GenChartStrategyEnum.GEN_SYNC.getValue())
public class GenChartSync implements GenChartStrategy {

    @Resource
    ChartService chartService;

    @Resource
    AiManager aiManager;

//    @Resource
//    ChartLogService logService;
    @Resource
    WebSocketServer webSocketServer;
    @Override
    public BiResponse executeGenChart(ChartEntity chartEntity) {
        // 系统预设 ( 简单预设 )
        /* 较好的做法是在系统（模型）层面做预设效果一般来说，会比直接拼接在用户消息里效果更好一些。*/
        /*
        分析需求：
        分析网站用户的增长情况
        原始数据：
        日期,用户数
        1号,10
        2号,20
        3号,30
        */
//        String result = aiManager.doChat(userInput.toString(), AIConstant.BI_MODEL_ID);
        try{
            String userInput = ChartUtil.buildUserInput(chartEntity);
            String result = aiManager.doChat(userInput, AIConstant.BI_MODEL_ID);
            String[] split = result.split("【【【【【");
            // 第一个是 空字符串
            if (split.length < 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误!");
            }
            // 图表代码
            String genChart = split[1].trim();
            // 分析结果
            String genResult = split[2].trim();
            // 更新数据到数据库
//            chartEntity.setGenChart(genChart);
//            chartEntity.setGenResult(genResult);
            chartEntity.setStatus(ChartStatusEnum.SUCCEED.getStatus());
            chartEntity.setExecMessage(ChartStatusEnum.SUCCEED.getMessage());
            chartEntity.setExecMessage("生成成功");
            genChart = ChartUtil.compressJson(genChart);
            boolean save = chartService.updateById(chartEntity);
            ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表保存失败!");
            //保存生成结果到mongo
            boolean syncResult = chartService.syncChart(chartEntity,genChart,genResult);
            ThrowUtils.throwIf(!syncResult, ErrorCode.SYSTEM_ERROR, "图表同步失败!");
            // 记录生成日志
//            logService.recordLog(chartEntity);
            // 封装返回结果
            BiResponse biResponse = new BiResponse();
            biResponse.setGenChart(genChart);
            biResponse.setChartId(chartEntity.getId());
            biResponse.setGenResult(genResult);
            webSocketServer.sendMessage("您的[" + chartEntity.getName() + "]生成成功 , 前往 我的图表 进行查看",
                    new HashSet<>(Arrays.asList(chartEntity.getUserId().toString())));
            return biResponse;
        } catch (BusinessException e) {
            // 更新状态信息
            ChartEntity updateChartResult = new ChartEntity();
            updateChartResult.setId(chartEntity.getId());
            updateChartResult.setStatus(ChartStatusEnum.FAILED.getStatus());
            updateChartResult.setExecMessage(e.getDescription());
            boolean updateResult = chartService.updateById(updateChartResult);
            // 记录生成日志
//            logService.recordLog(chartEntity);
            if (!updateResult) {
                log.info("更新图表FAILED状态信息失败 , chatId:{}", updateChartResult.getId());
            }
            // 抛出异常进行日志打印
            throw new GenChartException(chartEntity.getId(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
