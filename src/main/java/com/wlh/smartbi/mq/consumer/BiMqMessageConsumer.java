package com.wlh.smartbi.mq.consumer;


import com.rabbitmq.client.Channel;
import com.wlh.smartbi.common.ErrorCode;
import com.wlh.smartbi.common.constant.AIConstant;
import com.wlh.smartbi.common.constant.BiMqConstant;
import com.wlh.smartbi.common.exception.BusinessException;
import com.wlh.smartbi.common.exception.GenChartException;
import com.wlh.smartbi.manager.AiManager;
import com.wlh.smartbi.model.DO.ChartEntity;
import com.wlh.smartbi.model.enums.ChartStatusEnum;
import com.wlh.smartbi.service.ChartLogService;
import com.wlh.smartbi.service.ChartService;
import com.wlh.smartbi.utils.ChartUtil;
import com.wlh.smartbi.utils.ThrowUtils;
import com.wlh.smartbi.webSocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author WLH
 * @className BiMqMessageConsumer
 * @date : 2023/08/17/ 10:52
 **/
@Component
@Slf4j
public class BiMqMessageConsumer {

    @Resource
    AiManager aiManager;

    @Resource
    ChartService chartService;

    @Resource
    WebSocketServer webSocketServer;

//    @Resource
//    ChartLogService logService;

    //    @RabbitListener(queues = BiMqConstant.BI_QUEUE_NAME, ackMode = "MANUAL")
    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = BiMqConstant.BI_QUEUE_NAME), exchange = @Exchange(name = BiMqConstant.BI_EXCHANGE_NAME, type = ExchangeTypes.DIRECT), key = BiMqConstant.BI_ROUTING_KEY))
    @Retryable(value = GenChartException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000 * 60))
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliverTag) throws IOException {
        log.info("receive message :{}", message);
        if (StringUtils.isBlank(message)) {
            // reject message
            channel.basicNack(deliverTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接收到的消息为空!");
        }
        long chartId = Long.parseLong(message);
        ChartEntity chartEntity = chartService.getById(chartId);
        if (chartEntity == null) {
            // reject message
            channel.basicNack(deliverTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表为空!");
        }
        try {
            ChartEntity genChartEntity = new ChartEntity();
            genChartEntity.setId(chartId);
            genChartEntity.setStatus(ChartStatusEnum.RUNNING.getStatus());
            boolean b = chartService.updateById(genChartEntity);
            // throw异常
            ThrowUtils.throwIf(!b, new BusinessException(ErrorCode.SYSTEM_ERROR, "修改图表状态信息失败 " + chartId));
            String userInput = ChartUtil.buildUserInput(chartEntity);
            // 系统预设 ( 简单预设 )
            /* 较好的做法是在系统（模型）层面做预设效果一般来说，会比直接拼接在用户消息里效果更好一些。*/
            String result = aiManager.doChat(userInput.toString(), AIConstant.BI_MODEL_ID);
//            String goal = chartEntity.getGoal();
//            String csvData = chartEntity.getChartData();
//            String chartType = chartEntity.getChartType();
//            String result = aiManager.chatAndGenChart(goal, chartType, csvData);
            String[] split = result.split("【【【【【");
            // 第一个是 空字符串
            if (split.length < 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误!");
            }
            // 图表代码
            String genChart = split[1].trim();
            // 压缩JSON数据
            String compressedChart = ChartUtil.compressJson(genChart);
            // 分析结果
            String genResult = split[2].trim();
            // 更新数据
            chartEntity.setStatus(ChartStatusEnum.SUCCEED.getStatus());
            chartEntity.setExecMessage(ChartStatusEnum.SUCCEED.getMessage());
            // 保存数据到MongoDB
            boolean syncResult = chartService.syncChart(chartEntity,compressedChart,genResult);
            boolean updateGenResult = chartService.updateById(chartEntity);
            ThrowUtils.throwIf(!(updateGenResult && syncResult), ErrorCode.SYSTEM_ERROR, "生成图表保存失败!");            // 记录生成日志
//            logService.recordLog(chartEntity);

        } catch (BusinessException e) {
            // reject
            channel.basicNack(deliverTag, false, false);
            ChartEntity updateChartResult = new ChartEntity();
            updateChartResult.setId(chartId);
            updateChartResult.setStatus(ChartStatusEnum.FAILED.getStatus());
            updateChartResult.setExecMessage(e.getDescription());
            boolean updateResult = chartService.updateById(updateChartResult);
//            logService.recordLog(chartEntity);
            if (!updateResult) {
                log.info("更新图表FAILED状态信息失败 , chatId:{}", updateChartResult.getId());
            }
            // 抛出异常进行日志打印
            throw new GenChartException(chartId, e);
        }
        webSocketServer.sendMessage("您的[" + chartEntity.getName() + "]生成成功 , 前往 我的图表 进行查看", new HashSet<>(Arrays.asList(chartEntity.getUserId().toString())));
        channel.basicAck(deliverTag, false);
    }

    /**
     * 超过最重试次数上限
     *
     * @param e e
     */
    @Recover
    public void recoverFromMaxAttempts(GenChartException e) {
        boolean updateResult = chartService.update()
                .eq("id", e.getChartId())
                .set("status", ChartStatusEnum.FAILED.getStatus())
                .set("execMessage", "图表生成失败,系统已重试多次,请检查您的需求或数据。")
                .update();
        log.info(String.format("图表ID:%d 已超过最大重试次数, 已更新图表执行信息", e.getChartId()));
    }



}
