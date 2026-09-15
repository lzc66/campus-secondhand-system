package com.campus.secondhand;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 数据库 Schema 与代码的一致性集成测试(真实 MySQL 8):
 * 此前 admin_operation_logs 的 ENUM 与代码写入值不匹配,导致管理端大量写操作在严格模式下
 * 全部回滚,而纯 mock 的单元测试结构上发现不了这类问题——本测试就是防回归网。
 * 本机没有 Docker 时自动跳过(disabledWithoutDocker),在 CI/带 Docker 的环境执行。
 */
@Testcontainers(disabledWithoutDocker = true)
class DatabaseSchemaCompatibilityTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33");

    static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void setUpDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(MYSQL.getJdbcUrl());
        config.setUsername(MYSQL.getUsername());
        config.setPassword(MYSQL.getPassword());
        jdbcTemplate = new JdbcTemplate(new HikariDataSource(config));
        executeScript("database/schema.sql");
        executeScript("database/seeds/001_initial_seed.sql");
    }

    @Test
    void shouldAcceptEveryOperationTypeAndTargetTypeWrittenByCode() {
        // 与代码实际写入值一一对应:任何一行插入失败都说明 ENUM 又和代码脱节了
        List<String> targetTypes = List.of("registration", "user", "item", "comment", "wanted_post",
                "announcement", "order", "system_setting");
        List<String> operationTypes = List.of("approve", "reject", "disable", "enable", "edit", "delete",
                "restore", "publish", "offline", "other", "create", "update", "update_status", "cancel", "close");
        for (String targetType : targetTypes) {
            for (String operationType : operationTypes) {
                assertDoesNotThrow(() -> jdbcTemplate.update(
                        "INSERT INTO admin_operation_logs (admin_id, target_type, target_id, operation_type, operation_detail) "
                                + "VALUES (?, ?, ?, ?, ?)",
                        1L, targetType, 1L, operationType, "{}"),
                        "targetType=" + targetType + ", operationType=" + operationType);
            }
        }
    }

    @Test
    void shouldInsertEmailNotificationWithRetryCountColumn() {
        // 邮件重试机制依赖 retry_count 列,缺列时通知写入会直接失败
        assertDoesNotThrow(() -> jdbcTemplate.update(
                "INSERT INTO notifications (receiver_user_id, channel, business_type, title, content, send_status, retry_count) "
                        + "VALUES (NULL, 'email', 'system', 'test', 'test', 'pending', 0)"));
    }

    @Test
    void shouldSeedDefaultAdminAndCategories() {
        Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admins WHERE admin_no = 'admin1001'", Integer.class);
        assertEquals(1, adminCount);
        Integer categoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item_categories", Integer.class);
        assertEquals(6, categoryCount);
    }

    /**
     * 执行仪表盘全部聚合语句(与 Mapper 中 @Select 文本一致):
     * 表名/列名与 Schema 脱节(如把 orders 误写成 trade_orders)会在这里立刻失败,
     * 而不是等到管理员打开看板时才报 500。
     */
    @Test
    void shouldExecuteDashboardAggregateQueries() {
        jdbcTemplate.update("INSERT INTO users (student_no, email, password_hash, real_name, college_name, account_status) "
                + "VALUES ('20269999', 'agg@campus.local', 'hash', 'Agg Tester', 'School', 'active')");
        Long userId = jdbcTemplate.queryForObject("SELECT user_id FROM users WHERE student_no = '20269999'", Long.class);
        jdbcTemplate.update("INSERT INTO items (seller_user_id, category_id, title, description, condition_level, price, stock, status) "
                + "VALUES (?, 1, 'Agg Item', 'desc', 'new', 10.00, 1, 'on_sale')", userId);
        Long itemId = jdbcTemplate.queryForObject("SELECT item_id FROM items WHERE title = 'Agg Item'", Long.class);
        jdbcTemplate.update("INSERT INTO orders (order_no, buyer_user_id, seller_user_id, order_type, payment_method, payment_status, "
                        + "order_status, delivery_type, receiver_name, receiver_phone, delivery_address, total_amount, completed_at) "
                        + "VALUES ('ORD-AGG-1', ?, ?, 'online_cod', 'cod', 'paid', 'completed', 'face_to_face', 'Agg', '13800000000', 'Dorm', 10.00, NOW())",
                userId, userId);
        Long orderId = jdbcTemplate.queryForObject("SELECT order_id FROM orders WHERE order_no = 'ORD-AGG-1'", Long.class);
        jdbcTemplate.update("INSERT INTO order_items (order_id, item_id, item_title_snapshot, item_price_snapshot, quantity, subtotal_amount) "
                + "VALUES (?, ?, 'Agg Item', 10.00, 1, 10.00)", orderId, itemId);
        jdbcTemplate.update("INSERT INTO search_histories (user_id, keyword, category_id, searched_at) VALUES (?, 'agg', 1, NOW())", userId);

        java.sql.Timestamp start = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(1));
        java.sql.Timestamp end = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(1));

        assertDoesNotThrow(() -> jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE completed_at IS NOT NULL AND completed_at >= ?",
                java.math.BigDecimal.class, start));
        assertDoesNotThrow(() -> jdbcTemplate.queryForList(
                "SELECT DATE(created_at) AS d, COUNT(*) AS c FROM orders WHERE created_at >= ? AND created_at < ? GROUP BY DATE(created_at)",
                start, end));
        assertDoesNotThrow(() -> jdbcTemplate.queryForList(
                "SELECT DATE(completed_at) AS d, COUNT(*) AS c, COALESCE(SUM(total_amount), 0) AS a FROM orders "
                        + "WHERE completed_at IS NOT NULL AND completed_at >= ? AND completed_at < ? GROUP BY DATE(completed_at)",
                start, end));
        assertDoesNotThrow(() -> jdbcTemplate.queryForList(
                "SELECT DATE(cancelled_at) AS d, COUNT(*) AS c FROM orders "
                        + "WHERE cancelled_at IS NOT NULL AND cancelled_at >= ? AND cancelled_at < ? GROUP BY DATE(cancelled_at)",
                start, end));
        assertDoesNotThrow(() -> jdbcTemplate.queryForList(
                "SELECT i.category_id AS categoryId, MAX(c.category_name) AS categoryName, SUM(oi.quantity) AS soldQuantity, "
                        + "COUNT(DISTINCT oi.order_id) AS completedOrderCount, SUM(oi.subtotal_amount) AS completedAmount "
                        + "FROM order_items oi JOIN items i ON i.item_id = oi.item_id "
                        + "LEFT JOIN item_categories c ON c.category_id = i.category_id "
                        + "JOIN orders o ON o.order_id = oi.order_id "
                        + "WHERE o.completed_at IS NOT NULL AND o.completed_at >= ? AND o.completed_at < ? "
                        + "GROUP BY i.category_id ORDER BY completedAmount DESC LIMIT 10",
                start, end));
        assertDoesNotThrow(() -> jdbcTemplate.queryForList(
                "SELECT keyword AS keyword, category_id AS categoryId, COUNT(*) AS cnt FROM search_histories "
                        + "WHERE searched_at >= ? AND searched_at < ? AND category_id IS NOT NULL GROUP BY keyword, category_id",
                start, end));
        assertDoesNotThrow(() -> jdbcTemplate.queryForList("SELECT status AS status, COUNT(*) AS c FROM items GROUP BY status"));
        assertDoesNotThrow(() -> jdbcTemplate.queryForList(
                "SELECT DATE(created_at) AS d, COUNT(*) AS c FROM users WHERE deleted_at IS NULL AND created_at >= ? AND created_at < ? GROUP BY DATE(created_at)",
                start, end));
    }

    private static void executeScript(String path) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new FileSystemResource(path), StandardCharsets.UTF_8));
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to execute " + path, ex);
        }
    }
}
