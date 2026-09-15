package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 分类成交排行:SQL 内 JOIN + GROUP BY 聚合,
     * 替代"取回区间全部订单与订单项后在内存里累加、再拼巨型 IN"的实现。
     */
    @Select("""
            SELECT i.category_id AS categoryId,
                   MAX(c.category_name) AS categoryName,
                   SUM(oi.quantity) AS soldQuantity,
                   COUNT(DISTINCT oi.order_id) AS completedOrderCount,
                   SUM(oi.subtotal_amount) AS completedAmount
            FROM order_items oi
            JOIN items i ON i.item_id = oi.item_id
            LEFT JOIN item_categories c ON c.category_id = i.category_id
            JOIN trade_orders o ON o.order_id = oi.order_id
            WHERE o.completed_at IS NOT NULL
              AND o.completed_at >= #{start} AND o.completed_at < #{end}
            GROUP BY i.category_id
            ORDER BY completedAmount DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectCategorySalesRanking(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end,
                                                         @Param("limit") int limit);
}
