# Schema Design Notes

## Design assumptions

- Database engine: MySQL 8.0+ with InnoDB
- Character set: `utf8mb4`
- Workload: medium-scale campus OLTP application
- Consistency requirement: strong consistency for registration, item, order, and audit data
- Normalization target: 3NF, with selective denormalization only for counters and snapshots

## Functional module to table mapping

| Module | Main tables |
| --- | --- |
| User registration and approval | `registration_applications`, `users`, `admins`, `media_files`, `notifications` |
| User and admin login | `users`, `admins`, `login_logs` |
| Item publishing and management | `items`, `item_images`, `item_categories`, `media_files` |
| Online/offline trading | `orders`, `order_items`, `order_status_logs` |
| Personal center | `users`, `items`, `wanted_posts`, `item_comments`, `notifications` |
| Comments and replies | `item_comments`, `notifications` |
| Browse and search | `items`, `item_categories`, `search_histories` |
| Recommendation | `user_behavior_logs`, `user_recommendations`, `search_histories` |
| Wanted posts | `wanted_posts`, `item_categories` |
| Announcement and notice | `announcements`, `notifications` |
| Admin governance | `admin_operation_logs`, `admins`, `users`, `items`, `registration_applications` |

## Table details

### `admins`

Purpose:
Store administrator accounts. The `admin_no` field is constrained to the `admin` + digits pattern.

Key fields:
- `admin_no`: unique login account
- `role_code`: `super_admin`, `auditor`, `operator`
- `account_status`: admin account status

### `media_files`

Purpose:
Centralize uploaded image and document metadata, including student cards, avatars, and item pictures.

Key fields:
- `file_key`: unique storage key
- `file_url`: file access address
- `uploader_role`, `uploader_ref_id`: uploader source

### `registration_applications`

Purpose:
Store user registration requests before approval. Approved requests are transformed into active users.

Key fields:
- `application_no`: unique business serial number
- `student_no`, `email`: registration identity
- `student_card_file_id`: uploaded student card evidence
- `status`: `pending`, `approved`, `rejected`, `cancelled`
- `reviewer_admin_id`, `reviewed_at`, `review_remark`: approval trail

Constraints:
- One business application number maps to one row.
- The system should enforce only one pending application per student number at the service layer.

### `users`

Purpose:
Store approved student users.

Key fields:
- `student_no`: unique login identifier
- `password_hash`: encrypted password
- `phone`, `qq_no`, `wechat_no`: seller contact fields
- `dormitory_address`: dorm or pickup address
- `account_status`: `active`, `disabled`, `locked`

Notes:
- `application_id` provides traceability back to the approval request.
- `deleted_at` supports soft delete and audit-safe user management.

### `login_logs`

Purpose:
Keep both user and admin login attempts for audit, security analysis, and captcha troubleshooting.

Key fields:
- `account_type`: `user` or `admin`
- `login_result`: success or failure
- `captcha_passed`: whether captcha verification passed
- `ip_address`, `user_agent`: security trace fields

### `item_categories`

Purpose:
Hierarchical classification for browse pages and wanted posts.

Key fields:
- `parent_id`: self-referencing parent category
- `category_code`: stable unique identifier for backend logic
- `is_enabled`: whether the category is visible

Recommended initial categories:
- `digital_devices`
- `books_notes`
- `sports_goods`
- `dorm_supplies`
- `daily_use`
- `tickets_cards`

### `items`

Purpose:
Core second-hand listing table.

Key fields:
- `seller_user_id`: publisher
- `category_id`: browse category
- `title`, `brand`, `model`, `description`: search and detail display
- `condition_level`: item wear level
- `price`, `original_price`, `negotiable`: pricing model
- `trade_mode`: `offline`, `online`, `both`
- `contact_phone`, `contact_qq`, `contact_wechat`: direct-contact channels
- `status`: `draft`, `on_sale`, `reserved`, `sold`, `off_shelf`, `deleted`

Denormalized fields:
- `view_count`
- `comment_count`

Index strategy:
- `(category_id, status, price)` for browse and price filtering
- `(seller_user_id, status, created_at)` for personal center management
- full-text search on title, brand, model, description

### `item_images`

Purpose:
Store multiple pictures for each item and mark the cover image.

Key fields:
- `file_id`: reference to `media_files`
- `sort_order`: stable display order
- `is_cover`: cover image flag

### `item_comments`

Purpose:
Store first-level comments and replies under item detail pages.

Key fields:
- `parent_comment_id`: nested reply structure
- `reply_to_user_id`: direct reply target
- `status`: moderation or deletion state

Notes:
- Replies remain structurally valid even if parent comments are hidden, because parent foreign keys use `ON DELETE SET NULL`.

### `wanted_posts`

Purpose:
Store buy-request information when users cannot find what they need among active listings.

Key fields:
- `title`, `brand`, `model`, `description`: wanted demand content
- `expected_price_min`, `expected_price_max`: price range
- `status`: `open`, `matched`, `closed`, `cancelled`
- contact fields mirror item listings for consistency

Index strategy:
- user and category status indexes
- full-text search for demand matching

### `orders`

Purpose:
Store online cash-on-delivery transactions and optionally record offline deals in the future.

Key fields:
- `buyer_user_id`, `seller_user_id`: both sides of the transaction
- `order_type`: `online_cod`, `offline_record`
- `payment_method`: current design is fixed to `cod`
- `payment_status`, `order_status`: separated payment and business state
- `delivery_type`, `delivery_address`: dorm delivery support

Recommended state flow:
- `pending_confirm`
- `awaiting_delivery`
- `delivering`
- `completed`
- `cancelled`
- `closed`

### `order_items`

Purpose:
Store immutable snapshots of purchased items at order time.

Why snapshots are necessary:
- listing title or price may change later
- order history must remain stable

Key fields:
- `item_title_snapshot`
- `item_price_snapshot`
- `quantity`
- `subtotal_amount`

### `order_status_logs`

Purpose:
Store an audit trail of every order status change.

Key fields:
- `operator_type`: buyer, seller, admin, or system
- `from_status`, `to_status`
- `action_note`

### `search_histories`

Purpose:
Keep browse and search trails for recommendation features and operation analysis.

Key fields:
- `keyword`
- `category_id`
- `min_price`, `max_price`
- `sort_type`

Notes:
- `user_id` may be null for guests, matching the public browse requirement.

### `user_behavior_logs`

Purpose:
Capture user actions that can feed recommendation scoring and popularity metrics.

Behavior examples:
- `view`
- `search`
- `comment`
- `publish`
- `contact`
- `purchase`
- `want_post`

Recommended scoring direction:
- `purchase` > `contact` > `comment` > `view`

### `user_recommendations`

Purpose:
Store precomputed recommendation results so homepage and list pages can query quickly.

Key fields:
- `recommend_score`
- `reason_code`
- `generated_at`, `expires_at`
- `is_clicked`

Recommended usage:
- refresh periodically by batch job
- keep only recent valid recommendation windows

### `announcements`

Purpose:
Store system notices displayed on the homepage.

Key fields:
- `is_pinned`
- `publish_status`
- `published_at`, `expire_at`

### `notifications`

Purpose:
Unified notification table for both email delivery records and on-site messages.

Key fields:
- `channel`: `site` or `email`
- `business_type`: register review, order update, comment reply, announcement, system
- `send_status`
- `read_at`

Typical uses:
- registration approval or rejection email
- order status updates
- comment reply reminders

### `admin_operation_logs`

Purpose:
Provide accountability when administrators review registrations or manage users, items, comments, and orders.

Key fields:
- `target_type`
- `target_id`
- `operation_type`
- `operation_detail`

## Important integrity rules

1. Passwords are stored only as hashes, never plaintext.
2. User identity is split into `registration_applications` and `users`, so unapproved applicants cannot log in.
3. Item images are stored in a child table instead of fixed image columns, avoiding schema rigidity.
4. Order snapshots preserve historical accuracy after listing edits.
5. Soft-delete fields are kept on user-facing core tables to avoid accidental hard deletion.

## Suggested seed and master data

Initial master data to prepare before development:

- 1 super admin account
- root and child item categories
- homepage announcements for test data
- a few approved and rejected registration samples

## Future extension points

Likely future tables without breaking the current design:

- `item_favorites`
- `user_reports`
- `coupon_records`
- `chat_sessions`
- `chat_messages`
- `recommendation_models`
