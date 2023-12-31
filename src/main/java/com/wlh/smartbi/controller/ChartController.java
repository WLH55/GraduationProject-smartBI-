package com.wlh.smartbi.controller;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wlh.smartbi.common.BaseResponse;
import com.wlh.smartbi.common.ErrorCode;
import com.wlh.smartbi.common.annotation.AuthCheck;
import com.wlh.smartbi.common.constant.RedisConstant;
import com.wlh.smartbi.common.constant.UserConstant;
import com.wlh.smartbi.common.exception.BusinessException;
import com.wlh.smartbi.manager.RedisLimiterManager;
import com.wlh.smartbi.manager.StrategySelector;
import com.wlh.smartbi.model.DO.ChartEntity;
import com.wlh.smartbi.model.DO.UserEntity;
import com.wlh.smartbi.model.DTO.DeleteChartDocRequest;
import com.wlh.smartbi.model.DTO.DeleteRequest;
import com.wlh.smartbi.model.DTO.ServerLoadInfo;
import com.wlh.smartbi.model.DTO.chart.*;
import com.wlh.smartbi.model.DTO.user.UserDTO;
import com.wlh.smartbi.model.VO.ChartVO;
import com.wlh.smartbi.model.document.Chart;
import com.wlh.smartbi.model.enums.ChartStatusEnum;
import com.wlh.smartbi.mq.producer.BiMqMessageProducer;
import com.wlh.smartbi.service.ChartService;
import com.wlh.smartbi.service.GenChartStrategy;
import com.wlh.smartbi.service.UserService;
import com.wlh.smartbi.utils.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author WLH
 * @className ChartEntityController
 * @date : 2023/07/04/ 10:16
 **/
@RequestMapping("/chart")
@RestController
@Slf4j
@Api
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private BiMqMessageProducer biMqMessageProducer;

    @Resource
    StrategySelector selector;


    @PostMapping("/list/chart/unsucceed")
    @ApiOperation(value = "获取生成失败图表")
    public BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page> getUnsucceedChart(@RequestBody ChartQueryRequest chartQueryRequest) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserEntity loginUser = userService.getLoginUser();
        chartQueryRequest.setUserId(loginUser.getUserId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<ChartEntity> wrapper = chartService.getQueryWrapper(chartQueryRequest);
        wrapper.ne("status", ChartStatusEnum.SUCCEED.getStatus());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ChartEntity> page = chartService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size),
                wrapper);
        return ResultUtil.success(page);

    }

    @PostMapping("/list/chart/all")
    @ApiOperation(value = "获取所有图表")
    public BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page> getAllCharts(@RequestBody ChartQueryRequest chartQueryRequest) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserEntity loginUser = userService.getLoginUser();
        boolean isAdmin = userService.isAdmin(loginUser.getUserId());
//        boolean isBan = userService.isBan(loginUser.getUserId());
//        if(isBan){
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"你已被封号!请联系管理员2109236440@qq.com");
//        }
        if(!isAdmin){
            chartQueryRequest.setUserId(loginUser.getUserId());
        }
//        chartQueryRequest.setUserId(loginUser.getUserId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<ChartEntity> wrapper = chartService.getQueryWrapper(chartQueryRequest);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ChartEntity> page =
                chartService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size), wrapper);
        List<ChartVO> chartVOS = page.getRecords().stream().map(item -> {
            ChartVO chartVO = BeanUtil.copyProperties(item, ChartVO.class);
            return chartVO;
        }).collect(Collectors.toList());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page newPage = BeanUtil.copyProperties(page, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        newPage.setRecords(chartVOS);
//        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ChartVO> newPage=chartService.buildPage(page,chartVOS);
        return ResultUtil.success(newPage);
    }

    @GetMapping("/regen/chart")
    @ApiOperation(value = "重新生成图表")
    //String
    public BaseResponse<BiResponse> regenerateChart(@RequestParam("chartId") Long chartId) {
        // 取出数据
        ChartEntity chartEntity = chartService.getById(chartId);
        ThrowUtils.throwIf(chartEntity.getChartData().length() > 1000, ErrorCode.SYSTEM_ERROR, "原始信息过长!");
        // 获取用户信息
        UserEntity user = userService.getLoginUser();
        boolean isBan = userService.isBan(user.getUserId());
        if(isBan){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"你已被封号!请联系管理员2109236440@qq.com");
        }
        redisLimiterManager.doRateLimit(RedisConstant.GEN_CHART_LIMIT_KEY + user.getUserId());
        chartEntity.setStatus(ChartStatusEnum.WAIT.getStatus());
        chartEntity.setExecMessage(ChartStatusEnum.WAIT.getMessage());
        // 更新状态信息
        boolean updateById = chartService.updateById(chartEntity);
        ThrowUtils.throwIf(!updateById, ErrorCode.SYSTEM_ERROR, "重新生成图表失败");
        // 2. send to rabbitMQ
//        long newChartId = chartEntity.getId();
        // 在这里选择执行的策略
        //1. 获取当前执行状态
        ServerLoadInfo info = ServerMetricsUtil.getLoadInfo();
        //2. 获取执行策略
        GenChartStrategy genChartStrategy = selector.selectStrategy(info);
        //3. 执行生成图表
        BiResponse biResponse = genChartStrategy.executeGenChart(chartEntity);
//        biMqMessageProducer.sendGenChartMessage(String.valueOf(newChartId));
//        return ResultUtil.success("操作成功");
        return ResultUtil.success(biResponse);
    }

    /**
     * 智能图表(异步) : 消息队列
     *
     * @param multipartFile 数据文件
     * @param chartRequest  图要求
     * @return {@link BaseResponse}<{@link BiResponse}>
     */
    @PostMapping("/gen/async/mq")
    @ApiOperation(value = "通过ai生成图表")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAIRequest chartRequest) {
        // 1.save chat(Not Generated)
        // 取出数据
        String chartType = chartRequest.getChartType();
        String name = chartRequest.getName();
        String goal = chartRequest.getGoal();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空!");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType),ErrorCode.PARAMS_ERROR,"图表类型为空!");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长!");
        ExcelUtils.checkExcelFile(multipartFile);
        // 获取用户信息
        UserEntity user = userService.getLoginUser();
        boolean isBan = userService.isBan(user.getUserId());
        if(isBan){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"你已被封号!请联系管理员2109236440@qq.com");
        }
        redisLimiterManager.doRateLimit(RedisConstant.GEN_CHART_LIMIT_KEY + user.getUserId());
        // 读取文件信息
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        ThrowUtils.throwIf(csvData.length() > 10000, ErrorCode.SYSTEM_ERROR, "原始信息过长!");
        // 插入数据到数据库
        ChartEntity chartEntity = new ChartEntity();
        chartEntity.setUserId(user.getUserId());
        chartEntity.setName(name);
        chartEntity.setGoal(goal);
        chartEntity.setStatus(ChartStatusEnum.WAIT.getStatus());
        chartEntity.setChartType(chartType);
        chartEntity.setChartData(csvData);
        boolean save = chartService.save(chartEntity);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "保存图表失败!");
        // 在这里选择执行的策略
        //1. 获取当前执行状态
        ServerLoadInfo info = ServerMetricsUtil.getLoadInfo();
        //2. 获取执行策略
        GenChartStrategy genChartStrategy = selector.selectStrategy(info);
        //3. 执行生成图表
        BiResponse biResponse = genChartStrategy.executeGenChart(chartEntity);
        if (StringUtils.isNotBlank(biResponse.getGenChart())) {
            return ResultUtil.success(biResponse);
        }
        return ResultUtil.success(biResponse);
    }

    /**
     * 创建
     *
     * @param chartAddRequest
     * @return
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加图表实体")
    public BaseResponse<Long> addChartEntity(@RequestBody ChartAddRequest chartAddRequest) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ChartEntity chart = new ChartEntity();
        BeanUtils.copyProperties(chartAddRequest, chart);
        UserEntity loginUser = userService.getLoginUser();
        chart.setUserId(loginUser.getUserId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartEntityId = chart.getId();
        return ResultUtil.success(newChartEntityId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete/doc")
    @ApiOperation(value = "通过id删除图表")
    public BaseResponse<Boolean> deleteChartDocument(@RequestBody DeleteChartDocRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserEntity user = userService.getLoginUser();
        long id = deleteRequest.getId();
        // 判断是否存在
        ChartEntity oldChartEntity = chartService.getById(id);
        ThrowUtils.throwIf(oldChartEntity == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChartEntity.getUserId().equals(user.getUserId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.deleteSingleFromMongo(id,deleteRequest.getVersion());
        // 如果文档都被删除完那么就修改图表的状态WAIT
        if (chartService.getChartByChartId(oldChartEntity.getId()) == null) {
            oldChartEntity.setStatus(ChartStatusEnum.WAIT.getStatus());
            oldChartEntity.setExecMessage(ChartStatusEnum.WAIT.getMessage());
            chartService.updateById(oldChartEntity);
        }
        return ResultUtil.success(result);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete/do")
    @ApiOperation(value = "通过id删除图表")
    public BaseResponse<Boolean> deleteChartEntity(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserDTO user = UserHolder.getUser();
        long id = deleteRequest.getId();
        // 判断是否存在
        ChartEntity oldChartEntity = chartService.getById(id);
        ThrowUtils.throwIf(oldChartEntity == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChartEntity.getUserId().equals(user.getUserId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.deleteAllFromMongo(id);
        boolean b = chartService.removeById(id);
        return ResultUtil.success(b && result);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @ApiOperation(value = "更新图表实体内容")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChartEntity(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ChartEntity chart = new ChartEntity();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        ChartEntity oldChartEntity = chartService.getById(id);
        ThrowUtils.throwIf(oldChartEntity == null, ErrorCode.NOT_FOUND_ERROR);
        // 查看用户是否修改了原始数据 : 重新提交到AI服务进行图表生成
        if (!oldChartEntity.getChartData().equals(chartUpdateRequest.getChartData())) {
            // 发送消息到AI生成模块重新进行生成
            biMqMessageProducer.sendGenChartMessage(String.valueOf(oldChartEntity.getId()));
        }
        boolean result = chartService.updateById(chart);
        return ResultUtil.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @ApiOperation(value = "通过chartId获取图表")
    public BaseResponse<Chart> getChartEntityById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getChartByChartId(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtil.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @return
     */

    @PostMapping("/list/page")
    @ApiOperation(value = "获取图表列表")
    public BaseResponse<Page<Chart>> listChartEntityByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> charts = chartService.getChartList(chartQueryRequest);
        return ResultUtil.success(charts);
    }
    //管理员可以获取所有的图表
//    @PostMapping("/list/page/admin")
//    @ApiOperation(value = "获取图表列表")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Page<Chart>> listChartEntityByPageAdmin(@RequestBody ChartQueryRequest chartQueryRequest) {
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> charts = chartService.getChartList(chartQueryRequest);
//        return ResultUtil.success(charts);
//    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/my/list/page")
    @ApiOperation(value = "获取当前用户图表列表")
    public BaseResponse<Page<Chart>> listMyChartEntityByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserEntity loginUser = userService.getLoginUser();
        long userid = loginUser.getUserId();
        boolean isAdmin = userService.isAdmin(loginUser.getUserId());
        if(!isAdmin){
            chartQueryRequest.setUserId(userid);
        }
//        chartQueryRequest.setUserId(loginUser.getUserId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> charts = chartService.getChartList(chartQueryRequest);
        return ResultUtil.success(charts);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation(value = "编辑图表实体类")
    public BaseResponse<Boolean> editChartEntity(@RequestBody ChartEditRequest chartEditRequest) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ChartEntity chart = new ChartEntity();
        BeanUtils.copyProperties(chartEditRequest, chart);
        UserEntity loginUser = userService.getLoginUser();
        long id = chartEditRequest.getId();
        // 判断是否存在
        ChartEntity oldChartEntity = chartService.getById(id);
        ThrowUtils.throwIf(oldChartEntity == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChartEntity.getUserId().equals(loginUser.getUserId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        // 查看用户是否修改了原始数据 : 重新提交到AI服务进行图表生成
        if (!oldChartEntity.getChartData().equals(chartEditRequest.getChartData())) {
            // 发送消息到AI生成模块重新进行生成
            biMqMessageProducer.sendGenChartMessage(String.valueOf(oldChartEntity.getId()));
        }
        return ResultUtil.success(result);
    }

}
