package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.CancelledByType;
import com.campus.secondhand.enums.DeliveryType;
import com.campus.secondhand.enums.OrderStatus;
import com.campus.secondhand.enums.OrderType;
import com.campus.secondhand.enums.PaymentMethod;
import com.campus.secondhand.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("orders")
public class TradeOrder {

    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;
    private String orderNo;
    private Long buyerUserId;
    private Long sellerUserId;
    private OrderType orderType;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private DeliveryType deliveryType;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private String buyerRemark;
    private String sellerRemark;
    private CancelledByType cancelledBy;
    private String cancelReason;
    private LocalDateTime confirmedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
