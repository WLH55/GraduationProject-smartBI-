package com.wlh.smartbi.model.DTO;

import com.wlh.smartbi.common.constant.CommonConstant;
import lombok.Data;

/**
 * @author WLH
 * @className PageRequest
 * @date : 2023/07/04/ 10:33
 **/
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}
