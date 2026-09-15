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

    private static void executeScript(String path) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new FileSystemResource(path), StandardCharsets.UTF_8));
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to execute " + path, ex);
        }
    }
}
