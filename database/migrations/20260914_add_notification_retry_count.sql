-- 2026-09-14: notifications 表增加 retry_count 列,支持邮件失败重投(EmailRetryJob)。
-- 仅需在已存在的数据库上执行一次;新建数据库直接使用更新后的 schema.sql 即可。

USE campus_secondhand;

ALTER TABLE notifications
    ADD COLUMN retry_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Email delivery retry count' AFTER send_status;
