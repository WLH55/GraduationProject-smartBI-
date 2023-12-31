package com.wlh.smartbi.common;

/**
 * @author wlh
 * @className ErrorCode
 * @date : 2023/01/07/ 14:34
 **/
public enum ErrorCode {
    SUCCESS(200,"ok","请求成功"),
    //HTTP状态码本身就是500，为什么500，因为你的业务里面抛异常 , 但是不应该让前端出现500，因为我们刚刚自己定义了一个业务异常，它应该返回40000
    PARAMS_ERROR(400,"error","请求参数错误"),
    NULL_ERROR(400,"error","请求数据为空"),
    NOT_LOGIN(401,"error","未登录"),
    NO_AUTH_ERROR(403,"error","无权限"),
    SYSTEM_ERROR(500,"error","请登录~"),
    NOT_FOUND(404,"error","访问路径错误"),
    OPERATION_ERROR(50001, "error","操作失败"),
    NOT_FOUND_ERROR(40400, "error","请求数据不存在"),
    TOO_MANY_REQUEST(42900,"error" ,"请求过于频繁"),
    FORBIDDEN_ERROR(40300,"error" ,"禁止访问"),
    ;
    final int code;
    final String message;
    final String description;
    ErrorCode(int code, String message , String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
