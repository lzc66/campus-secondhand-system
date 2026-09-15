-- 2026-09-14: 修复 admin_operation_logs 的 ENUM 定义与代码实际写入值不匹配的问题。
-- 原 ENUM 缺少代码实际写入的 'system_setting' / 'create' / 'update' / 'update_status' / 'cancel' / 'close'，
-- MySQL 严格模式下会导致管理端封禁用户、商品下架、订单取消/关闭、公告新建/编辑、SMTP 配置保存等操作
-- 全部因日志插入失败而回滚（错误 1265）。
-- 仅需在已存在的数据库上执行一次；新建数据库直接使用更新后的 schema.sql 即可。

USE campus_secondhand;

ALTER TABLE admin_operation_logs
    MODIFY target_type ENUM('registration', 'user', 'item', 'comment', 'wanted_post', 'announcement', 'order', 'system_setting') NOT NULL COMMENT 'Managed object type',
    MODIFY operation_type ENUM('approve', 'reject', 'disable', 'enable', 'edit', 'delete', 'restore', 'publish', 'offline', 'other', 'create', 'update', 'update_status', 'cancel', 'close') NOT NULL COMMENT 'Operation type';
