USE simlect_order;

CREATE TABLE IF NOT EXISTS order_info (
                                          order_id varchar(32) NOT NULL PRIMARY KEY,
                                          amount decimal(10, 2) NULL,
                                          goods_amount decimal(10, 2) NULL,
                                          discount_amount decimal(10, 2) DEFAULT 0.00 NOT NULL,
                                          coupon_discount decimal(10, 2) DEFAULT 0.00 NOT NULL,
                                          user_coupon_id varchar(32) NULL,
                                          coupon_id varchar(20) NULL,
                                          user_id varchar(15) NULL,
                                          order_time datetime NULL,
                                          order_status tinyint(1) NULL,
                                          pay_channel varchar(10) NULL,
                                          pay_scene varchar(20) NULL,
                                          pay_order_id varchar(32) NULL,
                                          channel_order_Id varchar(50) NULL,
                                          comment_status tinyint DEFAULT 0 NULL,
                                          subject varchar(200) NULL
) COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS order_item (
                                          order_item_id varchar(40) NOT NULL PRIMARY KEY,
                                          order_id varchar(32) NOT NULL,
                                          cover varchar(100) NULL,
                                          product_id varchar(15) NOT NULL,
                                          product_name varchar(200) NULL,
                                          property_value_id_hash varchar(32) NOT NULL,
                                          property_info varchar(150) NULL,
                                          item_amount decimal(10, 2) NULL,
                                          buy_count int NULL,
                                          order_item_status tinyint(1) NULL,
                                          remark varchar(300) NULL,
                                          refund_order_id varchar(32) NULL,
                                          refund_amount decimal(10, 2) NULL
) COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS order_comment (
                                             order_id varchar(32) NOT NULL PRIMARY KEY,
                                             product_id varchar(15) NOT NULL,
                                             comment_content varchar(300) NULL,
                                             comment_time datetime NULL,
                                             comment_images varchar(300) NULL,
                                             star int NULL,
                                             comment_biz_reply varchar(255) NULL,
                                             recomment_content varchar(300) NULL,
                                             recomment_time datetime NULL,
                                             recomment_images varchar(300) NULL,
                                             user_id varchar(15) NULL,
                                             property_info varchar(150) NULL,
                                             status tinyint DEFAULT 0 NULL
) COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS order_coupon_rel (
                                                id bigint AUTO_INCREMENT PRIMARY KEY,
                                                order_id varchar(32) NOT NULL,
                                                user_coupon_id varchar(32) NOT NULL,
                                                coupon_id varchar(20) NOT NULL,
                                                discount_amount decimal(10, 2) NOT NULL,
                                                create_time datetime NOT NULL,
                                                CONSTRAINT uk_order_user_coupon UNIQUE (order_id, user_coupon_id)
) COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS comment_report (
                                              report_id int AUTO_INCREMENT PRIMARY KEY,
                                              order_id varchar(64) NOT NULL,
                                              product_id varchar(64) NULL,
                                              reporter_user_id varchar(64) NULL,
                                              reason varchar(50) NOT NULL,
                                              detail varchar(500) NULL,
                                              comment_snapshot varchar(1000) NULL,
                                              status tinyint DEFAULT 0 NOT NULL,
                                              report_time datetime NULL,
                                              handle_time datetime NULL,
                                              handle_remark varchar(500) NULL
);

-- Transactional outbox and compensation log, in the order database.
CREATE TABLE IF NOT EXISTS local_message_outbox (
                                                    id bigint AUTO_INCREMENT PRIMARY KEY,
                                                    idempotency_key varchar(128) NOT NULL,
                                                    exchange_name varchar(64) NOT NULL,
                                                    routing_key varchar(64) NOT NULL,
                                                    payload_json mediumtext NOT NULL,
                                                    reliability_level varchar(16) DEFAULT 'STANDARD' NOT NULL,
                                                    status tinyint DEFAULT 0 NOT NULL,
                                                    retry_count int DEFAULT 0 NOT NULL,
                                                    error_message varchar(512) NULL,
                                                    create_time datetime NOT NULL,
                                                    update_time datetime NULL,
                                                    sent_time datetime NULL,
                                                    CONSTRAINT uk_outbox_idempotency UNIQUE (idempotency_key),
                                                    KEY idx_outbox_status_ctime (status, create_time)
) CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS mq_compensation_log (
                                                   log_id int AUTO_INCREMENT PRIMARY KEY,
                                                   idempotency_key varchar(128) NOT NULL,
                                                   `exchange` varchar(64) NOT NULL,
                                                   routing_key varchar(64) NOT NULL,
                                                   biz_scene varchar(32) NULL,
                                                   payload_json mediumtext NULL,
                                                   reliability_level varchar(16) DEFAULT 'HIGH' NOT NULL,
                                                   error_message varchar(512) NULL,
                                                   retry_count int DEFAULT 0 NOT NULL,
                                                   status int DEFAULT 0 NOT NULL,
                                                   create_time datetime NOT NULL,
                                                   update_time datetime NULL,
                                                   handle_time datetime NULL,
                                                   handle_remark varchar(512) NULL,
                                                   CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key)
) CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS order_logistics_info (
                                                    order_id varchar(32) NOT NULL PRIMARY KEY,
                                                    user_id varchar(15) NULL,
                                                    logistics_no varchar(128) NULL,
                                                    logistics_company varchar(100) NULL,
                                                    sender_name varchar(100) NULL,
                                                    sender_phone varchar(20) NULL,
                                                    sender_address varchar(500) NULL,
                                                    receiver_name varchar(100) NULL,
                                                    receiver_phone varchar(20) NULL,
                                                    receiver_address varchar(500) NULL,
                                                    logistics_status tinyint DEFAULT 0 NULL
) COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS order_logistics_info_record (
                                                           record_id int AUTO_INCREMENT PRIMARY KEY,
                                                           order_id varchar(32) NOT NULL,
                                                           record_time datetime NULL,
                                                           record_address varchar(150) NULL
) COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;