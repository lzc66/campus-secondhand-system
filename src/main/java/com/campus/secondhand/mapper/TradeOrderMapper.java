package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    @Select("""
            SELECT COALESCE(SUM(total_amount), 0)
            FROM orders
            WHERE completed_at IS NOT NULL AND completed_at >= #{since}
            """)
    BigDecimal selectCompletedAmountSince(@Param("since") LocalDateTime since);

    @Select("""
            SELECT DATE(created_at) AS d, COUNT(*) AS c
            FROM orders
            WHERE created_at >= #{start} AND created_at < #{end}
            GROUP BY DATE(created_at)
            """)
    List<Map<String, Object>> selectCreatedTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE(completed_at) AS d, COUNT(*) AS c, COALESCE(SUM(total_amount), 0) AS a
            FROM orders
            WHERE completed_at IS NOT NULL AND completed_at >= #{start} AND completed_at < #{end}
            GROUP BY DATE(completed_at)
            """)
    List<Map<String, Object>> selectCompletedTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE(cancelled_at) AS d, COUNT(*) AS c
            FROM orders
            WHERE cancelled_at IS NOT NULL AND cancelled_at >= #{start} AND cancelled_at < #{end}
            GROUP BY DATE(cancelled_at)
            """)
    List<Map<String, Object>> selectCancelledTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
