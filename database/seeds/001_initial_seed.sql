USE campus_secondhand;

SET NAMES utf8mb4;

-- 使用 INSERT IGNORE:重复执行 seed 时跳过已存在的行,不覆盖任何字段。
-- 此前用 ON DUPLICATE KEY UPDATE 会把管理员改过的密码重置回默认值、把停用的账号重新激活,
-- 也会回滚管理员对分类的人工调整,属于重置后门,已移除。
INSERT IGNORE INTO item_categories (parent_id, category_code, category_name, sort_order, is_enabled)
VALUES
    (NULL, 'digital_devices', CONVERT(0xE695B0E7A081E8AEBEE5A487 USING utf8mb4), 10, 1),
    (NULL, 'books_notes', CONVERT(0xE69599E69D90E4B9A6E7B18D USING utf8mb4), 20, 1),
    (NULL, 'sports_goods', CONVERT(0xE8BF90E58AA8E794A8E59381 USING utf8mb4), 30, 1),
    (NULL, 'dorm_supplies', CONVERT(0xE5AEBFE8888DE794A8E59381 USING utf8mb4), 40, 1),
    (NULL, 'daily_use', CONVERT(0xE697A5E5B8B8E7949FE6B4BB USING utf8mb4), 50, 1),
    (NULL, 'tickets_cards', CONVERT(0xE7A5A8E588B8E58DA1E588B8 USING utf8mb4), 60, 1);

INSERT IGNORE INTO admins (
    admin_no,
    password_hash,
    admin_name,
    email,
    role_code,
    account_status
)
VALUES (
    'admin1001',
    '$2b$10$TlJI8z2zK0VwcUlz8h2X4e1/ffGwlwzN0ujwhpldA1Ks/0yVcMGpW',
    'Campus Admin',
    'admin@campus.local',
    'super_admin',
    'active'
);
