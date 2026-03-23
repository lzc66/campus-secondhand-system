-- Campus Second-hand Trading Management System
-- Database: MySQL 8.0+
-- Character set: utf8mb4
-- Design target: B/S architecture, OLTP, medium-scale campus marketplace

CREATE DATABASE IF NOT EXISTS campus_secondhand
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE campus_secondhand;

SET NAMES utf8mb4;

-- --------------------------------------------------------------------
-- 1. Administrator and user domain
-- --------------------------------------------------------------------

CREATE TABLE admins (
    admin_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    admin_no VARCHAR(32) NOT NULL COMMENT 'Login account, must start with admin',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Encrypted password hash',
    admin_name VARCHAR(50) NOT NULL COMMENT 'Administrator name',
    email VARCHAR(120) NOT NULL COMMENT 'Administrator email',
    role_code ENUM('super_admin', 'auditor', 'operator') NOT NULL DEFAULT 'operator' COMMENT 'Admin role',
    account_status ENUM('active', 'disabled') NOT NULL DEFAULT 'active' COMMENT 'Account status',
    last_login_at DATETIME NULL COMMENT 'Last successful login time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_admins_admin_no (admin_no),
    UNIQUE KEY uk_admins_email (email),
    CONSTRAINT chk_admins_admin_no CHECK (admin_no REGEXP '^admin[0-9]+$')
) ENGINE=InnoDB COMMENT='System administrators';

CREATE TABLE media_files (
    file_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    storage_provider VARCHAR(32) NOT NULL DEFAULT 'local' COMMENT 'Storage provider',
    bucket_name VARCHAR(64) NULL COMMENT 'Bucket or disk name',
    file_key VARCHAR(255) NOT NULL COMMENT 'Relative storage key',
    original_name VARCHAR(255) NOT NULL COMMENT 'Original filename',
    file_url VARCHAR(500) NOT NULL COMMENT 'Access URL',
    mime_type VARCHAR(100) NOT NULL COMMENT 'MIME type',
    file_size BIGINT UNSIGNED NOT NULL COMMENT 'File size in bytes',
    file_ext VARCHAR(20) NULL COMMENT 'File extension',
    file_category ENUM('image', 'document', 'other') NOT NULL DEFAULT 'image' COMMENT 'File category',
    uploader_role ENUM('guest', 'user', 'admin', 'system') NOT NULL DEFAULT 'guest' COMMENT 'Uploader role',
    uploader_ref_id BIGINT UNSIGNED NULL COMMENT 'Uploader business id',
    checksum_sha256 CHAR(64) NULL COMMENT 'Integrity checksum',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (file_id),
    UNIQUE KEY uk_media_files_file_key (file_key),
    KEY idx_media_files_uploader (uploader_role, uploader_ref_id),
    KEY idx_media_files_category_created (file_category, created_at)
) ENGINE=InnoDB COMMENT='Uploaded media and document metadata';

CREATE TABLE registration_applications (
    application_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    application_no VARCHAR(32) NOT NULL COMMENT 'Business application number',
    student_no VARCHAR(20) NOT NULL COMMENT 'Student number',
    real_name VARCHAR(50) NOT NULL COMMENT 'Real name',
    gender ENUM('male', 'female', 'unknown') NOT NULL DEFAULT 'unknown' COMMENT 'Gender',
    email VARCHAR(120) NOT NULL COMMENT 'Applicant email',
    phone VARCHAR(20) NULL COMMENT 'Applicant phone',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Encrypted password hash',
    college_name VARCHAR(100) NOT NULL COMMENT 'College or school',
    major_name VARCHAR(100) NULL COMMENT 'Major name',
    class_name VARCHAR(100) NULL COMMENT 'Class name',
    student_card_file_id BIGINT UNSIGNED NOT NULL COMMENT 'Student card image id',
    status ENUM('pending', 'approved', 'rejected', 'cancelled') NOT NULL DEFAULT 'pending' COMMENT 'Review status',
    reviewer_admin_id BIGINT UNSIGNED NULL COMMENT 'Reviewer admin id',
    review_remark VARCHAR(500) NULL COMMENT 'Review remark',
    reviewed_at DATETIME NULL COMMENT 'Reviewed time',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submitted time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (application_id),
    UNIQUE KEY uk_registration_applications_no (application_no),
    KEY idx_registration_student_status (student_no, status),
    KEY idx_registration_email_status (email, status),
    KEY idx_registration_status_submitted (status, submitted_at),
    KEY idx_registration_reviewer (reviewer_admin_id),
    CONSTRAINT fk_registration_student_card
        FOREIGN KEY (student_card_file_id) REFERENCES media_files(file_id),
    CONSTRAINT fk_registration_reviewer
        FOREIGN KEY (reviewer_admin_id) REFERENCES admins(admin_id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Registration applications waiting for admin approval';

CREATE TABLE users (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    application_id BIGINT UNSIGNED NULL COMMENT 'Approved registration application',
    student_no VARCHAR(20) NOT NULL COMMENT 'Student number used for login',
    email VARCHAR(120) NOT NULL COMMENT 'Email address',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Encrypted password hash',
    real_name VARCHAR(50) NOT NULL COMMENT 'Real name',
    gender ENUM('male', 'female', 'unknown') NOT NULL DEFAULT 'unknown' COMMENT 'Gender',
    phone VARCHAR(20) NULL COMMENT 'Phone number',
    qq_no VARCHAR(20) NULL COMMENT 'QQ id',
    wechat_no VARCHAR(64) NULL COMMENT 'Wechat id',
    avatar_file_id BIGINT UNSIGNED NULL COMMENT 'Avatar file id',
    college_name VARCHAR(100) NOT NULL COMMENT 'College or school',
    major_name VARCHAR(100) NULL COMMENT 'Major name',
    class_name VARCHAR(100) NULL COMMENT 'Class name',
    dormitory_address VARCHAR(255) NULL COMMENT 'Dormitory or pickup address',
    account_status ENUM('active', 'disabled', 'locked') NOT NULL DEFAULT 'active' COMMENT 'User status',
    last_login_at DATETIME NULL COMMENT 'Last successful login time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted_at DATETIME NULL COMMENT 'Soft delete time',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_application_id (application_id),
    UNIQUE KEY uk_users_student_no (student_no),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status_created (account_status, created_at),
    KEY idx_users_name_student (real_name, student_no),
    CONSTRAINT fk_users_application
        FOREIGN KEY (application_id) REFERENCES registration_applications(application_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_users_avatar
        FOREIGN KEY (avatar_file_id) REFERENCES media_files(file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Approved campus users';

CREATE TABLE login_logs (
    log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    account_type ENUM('user', 'admin') NOT NULL COMMENT 'Login account type',
    account_id BIGINT UNSIGNED NULL COMMENT 'Business account id, optional for failed login',
    login_name VARCHAR(64) NOT NULL COMMENT 'Student no or admin no',
    login_result ENUM('success', 'failure') NOT NULL COMMENT 'Login result',
    fail_reason VARCHAR(255) NULL COMMENT 'Reason for login failure',
    captcha_passed TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether captcha validation passed',
    ip_address VARCHAR(45) NULL COMMENT 'Client ip address',
    user_agent VARCHAR(255) NULL COMMENT 'Client agent',
    logged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Login time',
    PRIMARY KEY (log_id),
    KEY idx_login_logs_account (account_type, account_id, logged_at),
    KEY idx_login_logs_name_time (login_name, logged_at),
    KEY idx_login_logs_result_time (login_result, logged_at)
) ENGINE=InnoDB COMMENT='Audit log for user and admin login attempts';

-- --------------------------------------------------------------------
-- 2. Catalog and item publishing domain
-- --------------------------------------------------------------------

CREATE TABLE item_categories (
    category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    parent_id BIGINT UNSIGNED NULL COMMENT 'Parent category id',
    category_code VARCHAR(50) NOT NULL COMMENT 'Stable category code',
    category_name VARCHAR(50) NOT NULL COMMENT 'Category name',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Display order',
    is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether category is enabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_item_categories_code (category_code),
    KEY idx_item_categories_parent_sort (parent_id, sort_order),
    CONSTRAINT fk_item_categories_parent
        FOREIGN KEY (parent_id) REFERENCES item_categories(category_id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Hierarchical categories for items and wanted posts';

CREATE TABLE items (
    item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    seller_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Seller user id',
    category_id BIGINT UNSIGNED NOT NULL COMMENT 'Category id',
    title VARCHAR(150) NOT NULL COMMENT 'Item title',
    brand VARCHAR(80) NULL COMMENT 'Brand',
    model VARCHAR(80) NULL COMMENT 'Model',
    description TEXT NOT NULL COMMENT 'Detailed description',
    condition_level ENUM('new', 'almost_new', 'lightly_used', 'used', 'well_used') NOT NULL COMMENT 'Usage condition',
    price DECIMAL(10, 2) NOT NULL COMMENT 'Current selling price',
    original_price DECIMAL(10, 2) NULL COMMENT 'Original price',
    stock INT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Available stock',
    trade_mode ENUM('offline', 'online', 'both') NOT NULL DEFAULT 'both' COMMENT 'Supported trading mode',
    negotiable TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether price is negotiable',
    contact_phone VARCHAR(20) NULL COMMENT 'Contact phone',
    contact_qq VARCHAR(20) NULL COMMENT 'Contact QQ',
    contact_wechat VARCHAR(64) NULL COMMENT 'Contact Wechat',
    pickup_address VARCHAR(255) NULL COMMENT 'Offline pickup location',
    status ENUM('draft', 'on_sale', 'reserved', 'sold', 'off_shelf', 'deleted') NOT NULL DEFAULT 'on_sale' COMMENT 'Item status',
    published_at DATETIME NULL COMMENT 'Published time',
    sold_at DATETIME NULL COMMENT 'Sold time',
    view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'View count',
    comment_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Comment count',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted_at DATETIME NULL COMMENT 'Soft delete time',
    PRIMARY KEY (item_id),
    KEY idx_items_seller_status (seller_user_id, status, created_at),
    KEY idx_items_category_status_price (category_id, status, price),
    KEY idx_items_status_published (status, published_at),
    KEY idx_items_brand_model (brand, model),
    FULLTEXT KEY ft_items_search (title, brand, model, description),
    CONSTRAINT fk_items_seller
        FOREIGN KEY (seller_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_items_category
        FOREIGN KEY (category_id) REFERENCES item_categories(category_id)
) ENGINE=InnoDB COMMENT='Second-hand item listings';

CREATE TABLE item_images (
    item_image_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    item_id BIGINT UNSIGNED NOT NULL COMMENT 'Item id',
    file_id BIGINT UNSIGNED NOT NULL COMMENT 'Media file id',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Display order',
    is_cover TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether cover image',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (item_image_id),
    UNIQUE KEY uk_item_images_item_sort (item_id, sort_order),
    KEY idx_item_images_cover (item_id, is_cover),
    CONSTRAINT fk_item_images_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_item_images_file
        FOREIGN KEY (file_id) REFERENCES media_files(file_id)
) ENGINE=InnoDB COMMENT='Images of item listings';

CREATE TABLE item_comments (
    comment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    item_id BIGINT UNSIGNED NOT NULL COMMENT 'Item id',
    commenter_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Comment author',
    parent_comment_id BIGINT UNSIGNED NULL COMMENT 'Parent comment id',
    reply_to_user_id BIGINT UNSIGNED NULL COMMENT 'Reply target user id',
    content VARCHAR(1000) NOT NULL COMMENT 'Comment content',
    status ENUM('visible', 'hidden', 'deleted') NOT NULL DEFAULT 'visible' COMMENT 'Comment status',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted_at DATETIME NULL COMMENT 'Soft delete time',
    PRIMARY KEY (comment_id),
    KEY idx_item_comments_item_parent (item_id, parent_comment_id, created_at),
    KEY idx_item_comments_author (commenter_user_id, created_at),
    KEY idx_item_comments_reply_to (reply_to_user_id, created_at),
    CONSTRAINT fk_item_comments_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_item_comments_user
        FOREIGN KEY (commenter_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_item_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES item_comments(comment_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_item_comments_reply_to_user
        FOREIGN KEY (reply_to_user_id) REFERENCES users(user_id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Comments and replies on item detail pages';

CREATE TABLE wanted_posts (
    wanted_post_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    requester_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Requester user id',
    category_id BIGINT UNSIGNED NULL COMMENT 'Expected category id',
    title VARCHAR(150) NOT NULL COMMENT 'Wanted title',
    brand VARCHAR(80) NULL COMMENT 'Wanted brand',
    model VARCHAR(80) NULL COMMENT 'Wanted model',
    description TEXT NOT NULL COMMENT 'Requirement description',
    expected_price_min DECIMAL(10, 2) NULL COMMENT 'Expected minimum price',
    expected_price_max DECIMAL(10, 2) NULL COMMENT 'Expected maximum price',
    contact_phone VARCHAR(20) NULL COMMENT 'Contact phone',
    contact_qq VARCHAR(20) NULL COMMENT 'Contact QQ',
    contact_wechat VARCHAR(64) NULL COMMENT 'Contact Wechat',
    status ENUM('open', 'matched', 'closed', 'cancelled') NOT NULL DEFAULT 'open' COMMENT 'Wanted status',
    expires_at DATETIME NULL COMMENT 'Expiration time',
    view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'View count',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted_at DATETIME NULL COMMENT 'Soft delete time',
    PRIMARY KEY (wanted_post_id),
    KEY idx_wanted_posts_user_status (requester_user_id, status, created_at),
    KEY idx_wanted_posts_category_status (category_id, status, created_at),
    FULLTEXT KEY ft_wanted_posts_search (title, brand, model, description),
    CONSTRAINT fk_wanted_posts_user
        FOREIGN KEY (requester_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_wanted_posts_category
        FOREIGN KEY (category_id) REFERENCES item_categories(category_id)
        ON DELETE SET NULL,
    CONSTRAINT chk_wanted_posts_price
        CHECK (
            expected_price_max IS NULL
            OR expected_price_min IS NULL
            OR expected_price_max >= expected_price_min
        )
) ENGINE=InnoDB COMMENT='Wanted posts when users cannot find suitable items';

-- --------------------------------------------------------------------
-- 3. Trading domain
-- --------------------------------------------------------------------

CREATE TABLE orders (
    order_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    order_no VARCHAR(32) NOT NULL COMMENT 'Business order number',
    buyer_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Buyer user id',
    seller_user_id BIGINT UNSIGNED NOT NULL COMMENT 'Seller user id',
    order_type ENUM('online_cod', 'offline_record') NOT NULL DEFAULT 'online_cod' COMMENT 'Order type',
    payment_method ENUM('cod') NOT NULL DEFAULT 'cod' COMMENT 'Payment method',
    payment_status ENUM('unpaid', 'paid', 'cancelled') NOT NULL DEFAULT 'unpaid' COMMENT 'Payment status',
    order_status ENUM('pending_confirm', 'awaiting_delivery', 'delivering', 'completed', 'cancelled', 'closed') NOT NULL DEFAULT 'pending_confirm' COMMENT 'Order status',
    delivery_type ENUM('dorm_delivery', 'self_pickup', 'face_to_face') NOT NULL DEFAULT 'dorm_delivery' COMMENT 'Delivery type',
    receiver_name VARCHAR(50) NOT NULL COMMENT 'Receiver name',
    receiver_phone VARCHAR(20) NOT NULL COMMENT 'Receiver phone',
    delivery_address VARCHAR(255) NOT NULL COMMENT 'Dormitory or pickup address',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT 'Order total amount',
    buyer_remark VARCHAR(255) NULL COMMENT 'Buyer remark',
    seller_remark VARCHAR(255) NULL COMMENT 'Seller remark',
    cancelled_by ENUM('buyer', 'seller', 'admin', 'system') NULL COMMENT 'Order canceller',
    cancel_reason VARCHAR(255) NULL COMMENT 'Order cancel reason',
    confirmed_at DATETIME NULL COMMENT 'Seller confirmed time',
    delivered_at DATETIME NULL COMMENT 'Delivered time',
    completed_at DATETIME NULL COMMENT 'Completed time',
    cancelled_at DATETIME NULL COMMENT 'Cancelled time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_orders_order_no (order_no),
    KEY idx_orders_buyer_status (buyer_user_id, order_status, created_at),
    KEY idx_orders_seller_status (seller_user_id, order_status, created_at),
    KEY idx_orders_status_created (order_status, created_at),
    CONSTRAINT fk_orders_buyer
        FOREIGN KEY (buyer_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_orders_seller
        FOREIGN KEY (seller_user_id) REFERENCES users(user_id),
    CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB COMMENT='Orders for online purchase and optional offline records';

CREATE TABLE order_items (
    order_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    order_id BIGINT UNSIGNED NOT NULL COMMENT 'Order id',
    item_id BIGINT UNSIGNED NOT NULL COMMENT 'Source item id',
    item_title_snapshot VARCHAR(150) NOT NULL COMMENT 'Title snapshot at order time',
    item_price_snapshot DECIMAL(10, 2) NOT NULL COMMENT 'Price snapshot at order time',
    quantity INT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Purchased quantity',
    subtotal_amount DECIMAL(10, 2) NOT NULL COMMENT 'Subtotal amount',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (order_item_id),
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_item (item_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_item
        FOREIGN KEY (item_id) REFERENCES items(item_id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_subtotal CHECK (subtotal_amount >= 0)
) ENGINE=InnoDB COMMENT='Order item snapshots';

CREATE TABLE order_status_logs (
    order_status_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    order_id BIGINT UNSIGNED NOT NULL COMMENT 'Order id',
    operator_type ENUM('buyer', 'seller', 'admin', 'system') NOT NULL COMMENT 'Operator type',
    operator_id BIGINT UNSIGNED NULL COMMENT 'Operator business id',
    from_status VARCHAR(32) NULL COMMENT 'Previous status',
    to_status VARCHAR(32) NOT NULL COMMENT 'New status',
    action_note VARCHAR(255) NULL COMMENT 'Operation note',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (order_status_log_id),
    KEY idx_order_status_logs_order_time (order_id, created_at),
    CONSTRAINT fk_order_status_logs_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Order status change history';

-- --------------------------------------------------------------------
-- 4. Search, recommendation, and operation domain
-- --------------------------------------------------------------------

CREATE TABLE search_histories (
    search_history_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NULL COMMENT 'Searching user id, null for guest',
    keyword VARCHAR(100) NULL COMMENT 'Keyword input',
    category_id BIGINT UNSIGNED NULL COMMENT 'Selected category',
    min_price DECIMAL(10, 2) NULL COMMENT 'Minimum price filter',
    max_price DECIMAL(10, 2) NULL COMMENT 'Maximum price filter',
    sort_type ENUM('default', 'latest', 'price_asc', 'price_desc') NOT NULL DEFAULT 'default' COMMENT 'Sort type',
    searched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Search time',
    PRIMARY KEY (search_history_id),
    KEY idx_search_histories_user_time (user_id, searched_at),
    KEY idx_search_histories_keyword_time (keyword, searched_at),
    CONSTRAINT fk_search_histories_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_search_histories_category
        FOREIGN KEY (category_id) REFERENCES item_categories(category_id)
        ON DELETE SET NULL,
    CONSTRAINT chk_search_histories_price
        CHECK (
            max_price IS NULL
            OR min_price IS NULL
            OR max_price >= min_price
        )
) ENGINE=InnoDB COMMENT='Search records for browsing analysis and recommendation input';

CREATE TABLE user_behavior_logs (
    behavior_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Behavior owner',
    item_id BIGINT UNSIGNED NULL COMMENT 'Related item id',
    wanted_post_id BIGINT UNSIGNED NULL COMMENT 'Related wanted post id',
    behavior_type ENUM('view', 'search', 'comment', 'publish', 'contact', 'purchase', 'want_post') NOT NULL COMMENT 'Behavior type',
    source_page VARCHAR(50) NULL COMMENT 'Source page',
    search_keyword VARCHAR(100) NULL COMMENT 'Search keyword when behavior is search',
    weight DECIMAL(6, 2) NOT NULL DEFAULT 1.00 COMMENT 'Recommendation weight',
    extra_json JSON NULL COMMENT 'Extended behavior data',
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred time',
    PRIMARY KEY (behavior_log_id),
    KEY idx_user_behavior_user_time (user_id, occurred_at),
    KEY idx_user_behavior_item_type (item_id, behavior_type, occurred_at),
    KEY idx_user_behavior_wanted_type (wanted_post_id, behavior_type, occurred_at),
    CONSTRAINT fk_user_behavior_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_behavior_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_user_behavior_wanted
        FOREIGN KEY (wanted_post_id) REFERENCES wanted_posts(wanted_post_id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='User behavior logs feeding recommendation strategies';

CREATE TABLE user_recommendations (
    recommendation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Recommended user',
    item_id BIGINT UNSIGNED NOT NULL COMMENT 'Recommended item',
    recommend_score DECIMAL(8, 4) NOT NULL COMMENT 'Recommendation score',
    reason_code ENUM('similar_category', 'recent_view', 'hot_sale', 'keyword_match', 'collaborative_filtering', 'manual') NOT NULL COMMENT 'Recommendation reason',
    is_clicked TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether user clicked the recommendation',
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Generated time',
    expires_at DATETIME NULL COMMENT 'Recommendation expiration time',
    PRIMARY KEY (recommendation_id),
    KEY idx_user_recommendations_user_score (user_id, recommend_score, generated_at),
    KEY idx_user_recommendations_item (item_id),
    CONSTRAINT fk_user_recommendations_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_recommendations_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Materialized recommendation results for logged-in users';

CREATE TABLE announcements (
    announcement_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    publisher_admin_id BIGINT UNSIGNED NOT NULL COMMENT 'Publisher admin id',
    title VARCHAR(200) NOT NULL COMMENT 'Announcement title',
    content TEXT NOT NULL COMMENT 'Announcement content',
    is_pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether pinned on homepage',
    publish_status ENUM('draft', 'published', 'offline') NOT NULL DEFAULT 'published' COMMENT 'Publish status',
    published_at DATETIME NULL COMMENT 'Publish time',
    expire_at DATETIME NULL COMMENT 'Expiration time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (announcement_id),
    KEY idx_announcements_status_time (publish_status, is_pinned, published_at),
    CONSTRAINT fk_announcements_admin
        FOREIGN KEY (publisher_admin_id) REFERENCES admins(admin_id)
) ENGINE=InnoDB COMMENT='Homepage announcements published by administrators';

CREATE TABLE notifications (
    notification_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    receiver_user_id BIGINT UNSIGNED NULL COMMENT 'Receiver user id',
    receiver_email VARCHAR(120) NULL COMMENT 'Receiver email for pre-user or email push',
    sender_admin_id BIGINT UNSIGNED NULL COMMENT 'Sender admin id',
    channel ENUM('site', 'email') NOT NULL COMMENT 'Delivery channel',
    business_type ENUM('register_review', 'order_update', 'comment_reply', 'announcement', 'system') NOT NULL COMMENT 'Business source',
    business_id BIGINT UNSIGNED NULL COMMENT 'Business record id',
    title VARCHAR(200) NOT NULL COMMENT 'Notification title',
    content TEXT NOT NULL COMMENT 'Notification content',
    send_status ENUM('pending', 'sent', 'failed') NOT NULL DEFAULT 'pending' COMMENT 'Delivery status',
    sent_at DATETIME NULL COMMENT 'Sent time',
    read_at DATETIME NULL COMMENT 'Read time for site messages',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (notification_id),
    KEY idx_notifications_user_status (receiver_user_id, send_status, created_at),
    KEY idx_notifications_email_status (receiver_email, send_status, created_at),
    KEY idx_notifications_business (business_type, business_id),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (receiver_user_id) REFERENCES users(user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_notifications_admin
        FOREIGN KEY (sender_admin_id) REFERENCES admins(admin_id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Site notifications and email delivery records';

CREATE TABLE admin_operation_logs (
    admin_operation_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    admin_id BIGINT UNSIGNED NULL COMMENT 'Operator admin id',
    target_type ENUM('registration', 'user', 'item', 'comment', 'wanted_post', 'announcement', 'order') NOT NULL COMMENT 'Managed object type',
    target_id BIGINT UNSIGNED NOT NULL COMMENT 'Managed object id',
    operation_type ENUM('approve', 'reject', 'disable', 'enable', 'edit', 'delete', 'restore', 'publish', 'offline', 'other') NOT NULL COMMENT 'Operation type',
    operation_detail JSON NULL COMMENT 'Structured operation detail',
    ip_address VARCHAR(45) NULL COMMENT 'Operator ip',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (admin_operation_log_id),
    KEY idx_admin_operation_logs_admin_time (admin_id, created_at),
    KEY idx_admin_operation_logs_target (target_type, target_id, created_at),
    CONSTRAINT fk_admin_operation_logs_admin
        FOREIGN KEY (admin_id) REFERENCES admins(admin_id)
        ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Administrative operation logs';
