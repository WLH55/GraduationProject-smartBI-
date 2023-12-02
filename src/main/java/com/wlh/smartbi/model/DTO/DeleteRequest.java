package com.wlh.smartbi.model.DTO;

import lombok.Data;

import java.io.Serializable;

/**
 * @author WLH
 * @className DeleteRequest
 * @date : 2023/07/04/ 10:27
 **/
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}