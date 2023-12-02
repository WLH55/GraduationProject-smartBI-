package com.wlh.smartbi.manager;


import com.wlh.smartbi.model.DTO.ServerLoadInfo;
import com.wlh.smartbi.model.enums.GenChartStrategyEnum;
import com.wlh.smartbi.service.GenChartStrategy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author WLH
 * @className StrategySelector
 * @date : 2023/08/30/ 12:07
 **/
@Component
public class StrategySelector {

    /**
     * Spring会自动将strategy接口的实现类注入到这个Map中，key为bean id,value值则为对应的策略实现类
     */
    @Resource
    Map<String, GenChartStrategy> strategyMap;

    /**
     * 选择对应的生成图表执行策略
     *
     * @param info 服务器当前负载信息
     * @return {@link GenChartStrategy}
     */
    public GenChartStrategy selectStrategy(ServerLoadInfo info) {
        if (info.isVeryHighLoad()) {
            return strategyMap.get(GenChartStrategyEnum.GEN_REJECT.getValue());
        } else if (info.isHighLoad()) {
            return strategyMap.get(GenChartStrategyEnum.GEN_MQ.getValue());
        } else if (info.isMediumLoad()) {
            return strategyMap.get(GenChartStrategyEnum.GEN_THREAD_POOL.getValue());
        } else {
            return strategyMap.get(GenChartStrategyEnum.GEN_SYNC.getValue());
        }
    }


}
