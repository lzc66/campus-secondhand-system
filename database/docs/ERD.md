# Database ERD

## Overall scope

This schema is designed for a campus second-hand trading system under a B/S architecture.
The design keeps the OLTP core in 3NF, while allowing a few controlled denormalized fields
such as `items.view_count`, `items.comment_count`, and snapshot fields in `order_items`.

## Core ERD

```mermaid
erDiagram
    admins ||--o{ registration_applications : reviews
    registration_applications ||--o| users : creates
    media_files ||--o{ registration_applications : stores_card_image
    media_files ||--o{ users : stores_avatar

    users ||--o{ items : publishes
    item_categories ||--o{ items : classifies
    items ||--o{ item_images : has
    media_files ||--o{ item_images : stores_image

    users ||--o{ item_comments : writes
    items ||--o{ item_comments : receives
    item_comments ||--o{ item_comments : replies_to

    users ||--o{ wanted_posts : creates
    item_categories ||--o{ wanted_posts : classifies

    users ||--o{ orders : buys
    users ||--o{ orders : sells
    orders ||--|{ order_items : contains
    items ||--o{ order_items : snapshots
    orders ||--o{ order_status_logs : records

    users ||--o{ notifications : receives
    admins ||--o{ notifications : sends
    admins ||--o{ announcements : publishes

    users ||--o{ search_histories : searches
    users ||--o{ user_behavior_logs : creates
    items ||--o{ user_behavior_logs : feeds
    wanted_posts ||--o{ user_behavior_logs : feeds
    users ||--o{ user_recommendations : owns
    items ||--o{ user_recommendations : recommends

    admins ||--o{ admin_operation_logs : writes

    admins {
        bigint admin_id PK
        varchar admin_no UK
        varchar email UK
        enum role_code
        enum account_status
    }

    registration_applications {
        bigint application_id PK
        varchar application_no UK
        varchar student_no
        varchar email
        bigint student_card_file_id FK
        enum status
        bigint reviewer_admin_id FK
    }

    users {
        bigint user_id PK
        bigint application_id UK
        varchar student_no UK
        varchar email UK
        bigint avatar_file_id FK
        enum account_status
    }

    item_categories {
        bigint category_id PK
        bigint parent_id FK
        varchar category_code UK
        varchar category_name
    }

    items {
        bigint item_id PK
        bigint seller_user_id FK
        bigint category_id FK
        varchar title
        decimal price
        enum condition_level
        enum trade_mode
        enum status
    }

    item_comments {
        bigint comment_id PK
        bigint item_id FK
        bigint commenter_user_id FK
        bigint parent_comment_id FK
        bigint reply_to_user_id FK
        enum status
    }

    wanted_posts {
        bigint wanted_post_id PK
        bigint requester_user_id FK
        bigint category_id FK
        varchar title
        decimal expected_price_min
        decimal expected_price_max
        enum status
    }

    orders {
        bigint order_id PK
        varchar order_no UK
        bigint buyer_user_id FK
        bigint seller_user_id FK
        decimal total_amount
        enum order_status
        enum payment_status
    }

    order_items {
        bigint order_item_id PK
        bigint order_id FK
        bigint item_id FK
        varchar item_title_snapshot
        decimal item_price_snapshot
        int quantity
    }

    notifications {
        bigint notification_id PK
        bigint receiver_user_id FK
        bigint sender_admin_id FK
        enum channel
        enum business_type
        enum send_status
    }

    user_behavior_logs {
        bigint behavior_log_id PK
        bigint user_id FK
        bigint item_id FK
        bigint wanted_post_id FK
        enum behavior_type
        decimal weight
    }

    user_recommendations {
        bigint recommendation_id PK
        bigint user_id FK
        bigint item_id FK
        decimal recommend_score
        enum reason_code
    }
```

## Module notes

### Registration and approval

- `registration_applications` stores all pending, approved, rejected, and cancelled registration requests.
- `users` is created only after approval.
- `notifications` supports both on-site and email notifications for review results.

### Item publishing and browsing

- `items` is the core listing table.
- `item_images` supports multiple pictures and a cover image.
- `item_categories` is reusable for both sale listings and wanted posts.
- Full-text indexes are provided for item title, brand, model, and description search.

### Trading

- `orders` stores order headers for online cash-on-delivery purchases.
- `order_items` stores immutable listing snapshots, protecting history if a listing later changes.
- `order_status_logs` stores the order state transition trail.

### Recommendation

- `search_histories` and `user_behavior_logs` are the main upstream data sources.
- `user_recommendations` stores materialized recommendation results for fast homepage retrieval.
