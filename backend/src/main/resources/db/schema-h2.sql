-- 演示模式（H2 内存库）建表脚本，由 Spring Boot 启动时自动执行
CREATE TABLE t_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(64) NOT NULL,
  nickname VARCHAR(50),
  phone VARCHAR(20),
  avatar VARCHAR(255),
  gender TINYINT DEFAULT 1,
  role VARCHAR(20) DEFAULT 'USER',
  status TINYINT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE t_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  icon VARCHAR(50),
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME
);

CREATE TABLE t_merchant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  logo VARCHAR(255),
  description VARCHAR(500),
  contact VARCHAR(32),
  phone VARCHAR(20),
  address VARCHAR(255),
  status TINYINT DEFAULT 1,
  balance DECIMAL(10,2) DEFAULT 0.00,
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE t_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  subtitle VARCHAR(200),
  main_image VARCHAR(255),
  images VARCHAR(1000),
  detail CLOB,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2),
  stock INT DEFAULT 0,
  sales INT DEFAULT 0,
  unit VARCHAR(20) DEFAULT '件',
  rating DECIMAL(2,1) DEFAULT 5.0,
  review_count INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE t_cart_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT DEFAULT 1,
  checked TINYINT DEFAULT 1,
  create_time DATETIME
);

CREATE TABLE t_address (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  receiver VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  province VARCHAR(50),
  city VARCHAR(50),
  district VARCHAR(50),
  detail VARCHAR(255),
  is_default TINYINT DEFAULT 0,
  create_time DATETIME
);

CREATE TABLE t_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  merchant_name VARCHAR(64),
  total_amount DECIMAL(10,2) NOT NULL,
  freight DECIMAL(10,2) DEFAULT 0.00,
  discount DECIMAL(10,2) DEFAULT 0.00,
  pay_amount DECIMAL(10,2) NOT NULL,
  status TINYINT DEFAULT 0,
  receiver VARCHAR(50),
  phone VARCHAR(20),
  address VARCHAR(255),
  remark VARCHAR(255),
  pay_time DATETIME,
  ship_time DATETIME,
  finish_time DATETIME,
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE t_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32),
  merchant_id BIGINT,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(100),
  product_image VARCHAR(255),
  price DECIMAL(10,2),
  quantity INT,
  total_price DECIMAL(10,2)
);

CREATE TABLE t_banner (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100),
  image VARCHAR(255),
  link VARCHAR(255),
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME
);

CREATE TABLE t_review (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  user_name VARCHAR(50),
  user_avatar VARCHAR(255),
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32),
  merchant_id BIGINT,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(100),
  product_image VARCHAR(255),
  rating TINYINT DEFAULT 5,
  content VARCHAR(1000),
  images VARCHAR(1000),
  anonymous TINYINT DEFAULT 0,
  reply VARCHAR(500),
  reply_time DATETIME,
  create_time DATETIME
);

CREATE TABLE t_consult (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  user_name VARCHAR(50),
  user_avatar VARCHAR(255),
  merchant_id BIGINT NOT NULL,
  shop_name VARCHAR(64),
  shop_logo VARCHAR(255),
  product_id BIGINT,
  product_name VARCHAR(100),
  product_image VARCHAR(255),
  last_content VARCHAR(500),
  last_sender VARCHAR(16),
  last_time DATETIME,
  user_unread INT DEFAULT 0,
  merchant_unread INT DEFAULT 0,
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE t_consult_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  consult_id BIGINT NOT NULL,
  sender_type VARCHAR(16) NOT NULL,
  sender_id BIGINT,
  sender_name VARCHAR(50),
  content VARCHAR(1000),
  create_time DATETIME
);

CREATE TABLE t_logistics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32),
  tracking_no VARCHAR(64),
  company VARCHAR(50),
  content VARCHAR(255),
  create_time DATETIME
);

CREATE TABLE t_coupon (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  type TINYINT DEFAULT 1,
  threshold DECIMAL(10,2) DEFAULT 0.00,
  amount DECIMAL(10,2) NOT NULL,
  total INT DEFAULT 0,
  received INT DEFAULT 0,
  limit_per_user INT DEFAULT 1,
  valid_days INT DEFAULT 30,
  status TINYINT DEFAULT 1,
  description VARCHAR(255),
  create_time DATETIME
);

CREATE TABLE t_user_coupon (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  name VARCHAR(100),
  type TINYINT DEFAULT 1,
  threshold DECIMAL(10,2) DEFAULT 0.00,
  amount DECIMAL(10,2) NOT NULL,
  status TINYINT DEFAULT 0,
  receive_time DATETIME,
  expire_time DATETIME,
  use_time DATETIME,
  order_no VARCHAR(32)
);

CREATE TABLE t_withdraw (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  merchant_name VARCHAR(64),
  amount DECIMAL(10,2) NOT NULL,
  method TINYINT DEFAULT 1,
  account_no VARCHAR(64),
  account_name VARCHAR(32),
  status TINYINT DEFAULT 0,
  audit_remark VARCHAR(255),
  apply_time DATETIME,
  audit_time DATETIME,
  audit_by VARCHAR(64)
);

CREATE TABLE t_after_sale (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32),
  order_item_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  merchant_name VARCHAR(64),
  product_id BIGINT,
  product_name VARCHAR(128),
  product_image VARCHAR(255),
  price DECIMAL(10,2),
  quantity INT,
  amount DECIMAL(10,2),
  type TINYINT DEFAULT 1,
  reason VARCHAR(255),
  description VARCHAR(500),
  images VARCHAR(1000),
  status TINYINT DEFAULT 0,
  return_tracking_no VARCHAR(64),
  return_company VARCHAR(50),
  merchant_remark VARCHAR(255),
  refund_remark VARCHAR(255),
  apply_time DATETIME,
  merchant_handle_time DATETIME,
  return_time DATETIME,
  refund_time DATETIME,
  update_time DATETIME
);

-- ------------------------------------------------------------------
-- 网络安全与运维模块（访问审计 / 异常告警 / IP 黑名单）
-- ------------------------------------------------------------------

CREATE TABLE t_access_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uri VARCHAR(255),
  method VARCHAR(10),
  client_ip VARCHAR(64),
  user_id BIGINT,
  role VARCHAR(20),
  status_code INT,
  cost_ms BIGINT,
  user_agent VARCHAR(255),
  risk_flag TINYINT DEFAULT 0,
  create_time DATETIME
);
CREATE INDEX idx_access_log_time ON t_access_log(create_time);
CREATE INDEX idx_access_log_ip ON t_access_log(client_ip);

CREATE TABLE t_security_alert (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alert_type VARCHAR(40),
  level VARCHAR(10),
  metric_name VARCHAR(40),
  metric_value DECIMAL(12,2),
  baseline_value DECIMAL(12,2),
  score DECIMAL(8,2),
  detail VARCHAR(500),
  handled TINYINT DEFAULT 0,
  create_time DATETIME
);
CREATE INDEX idx_alert_time ON t_security_alert(create_time);

CREATE TABLE t_ip_blacklist (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ip VARCHAR(64),
  reason VARCHAR(200),
  source VARCHAR(20),
  expire_time DATETIME,
  create_time DATETIME
);
