package com.campus.secondhand.common.api;

import org.springframework.http.HttpStatus;

/**
 * 集中定义的业务错误码。此前错误码散落为魔法数字,且出现跨模块语义冲突
 * (40041/40042/40080/40980 在不同模块含义不同),前端无法建立稳定的错误码映射。
 * 新代码一律通过本枚举抛错:new BusinessException(ErrorCode.XXX)。
 */
public enum ErrorCode {

    // 管理端 - 商品管理
    ADMIN_ITEM_STATUS_REQUIRED(40041, HttpStatus.BAD_REQUEST, "itemStatus is required"),
    ADMIN_ITEM_STATUS_INVALID(40042, HttpStatus.BAD_REQUEST, "itemStatus must be on_sale, off_shelf, or deleted"),

    // 管理端 - SMTP 配置(原与商品管理复用 40041/40042,已拆分)
    SMTP_HOST_REQUIRED(40047, HttpStatus.BAD_REQUEST, "SMTP host is required when SMTP is enabled"),
    SMTP_PORT_INVALID(40048, HttpStatus.BAD_REQUEST, "SMTP port must be a positive number"),

    // 管理端 - 订单管理(原与用户端复用 40080/40980,已拆分)
    ADMIN_ORDER_STATUS_FILTER_INVALID(40083, HttpStatus.BAD_REQUEST, "orderStatus filter is invalid"),
    ADMIN_ORDER_CANNOT_CANCEL(40986, HttpStatus.CONFLICT, "Current order status cannot be cancelled by admin"),

    // 用户端 - 订单
    USER_ORDER_DELIVERY_TYPE_INVALID(40080, HttpStatus.BAD_REQUEST, "deliveryType is invalid"),
    USER_ORDER_CANNOT_BUY_OWN(40980, HttpStatus.CONFLICT, "You cannot buy your own item");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
