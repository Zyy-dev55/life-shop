-- ============================================================
-- 悦家生活 · 生活用品电商系统  数据库初始化脚本
-- MySQL 8.0 / utf8mb4  直接执行本文件即可完成建库建表与数据初始化
-- 账号：admin / admin123（平台管理员）  zhangsan / 123456（演示买家）
-- 商家：shop_kitchen / 123456（悦家厨房旗舰店）等 6 家，详见 README
-- ============================================================

DROP DATABASE IF EXISTS `life_shop`;
CREATE DATABASE `life_shop` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `life_shop`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `t_consult_message`;
DROP TABLE IF EXISTS `t_consult`;
DROP TABLE IF EXISTS `t_review`;
DROP TABLE IF EXISTS `t_order_item`;
DROP TABLE IF EXISTS `t_order`;
DROP TABLE IF EXISTS `t_address`;
DROP TABLE IF EXISTS `t_cart_item`;
DROP TABLE IF EXISTS `t_product`;
DROP TABLE IF EXISTS `t_merchant`;
DROP TABLE IF EXISTS `t_category`;
DROP TABLE IF EXISTS `t_banner`;
DROP TABLE IF EXISTS `t_user`;
SET FOREIGN_KEY_CHECKS = 1;

SET NAMES utf8mb4;

CREATE TABLE `t_user` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(64) NOT NULL,
  nickname VARCHAR(50),
  phone VARCHAR(20),
  avatar VARCHAR(255),
  gender TINYINT DEFAULT 1,
  role VARCHAR(20) DEFAULT 'USER',
  status TINYINT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_category` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  icon VARCHAR(50),
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_merchant` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  update_time DATETIME,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_product` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  subtitle VARCHAR(200),
  main_image VARCHAR(255),
  images VARCHAR(1000),
  detail TEXT,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2),
  stock INT DEFAULT 0,
  sales INT DEFAULT 0,
  unit VARCHAR(20) DEFAULT '件',
  rating DECIMAL(2,1) DEFAULT 5.0,
  review_count INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  KEY idx_category (category_id),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_cart_item` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT DEFAULT 1,
  checked TINYINT DEFAULT 1,
  create_time DATETIME,
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_address` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  receiver VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  province VARCHAR(50),
  city VARCHAR(50),
  district VARCHAR(50),
  detail VARCHAR(255),
  is_default TINYINT DEFAULT 0,
  create_time DATETIME,
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_order` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  update_time DATETIME,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_order_item` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32),
  merchant_id BIGINT,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(100),
  product_image VARCHAR(255),
  price DECIMAL(10,2),
  quantity INT,
  total_price DECIMAL(10,2),
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_banner` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100),
  image VARCHAR(255),
  link VARCHAR(255),
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_review` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  create_time DATETIME,
  UNIQUE KEY uk_order_product (order_id, product_id),
  KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_consult` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  update_time DATETIME,
  UNIQUE KEY uk_user_merchant (user_id, merchant_id),
  KEY idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_consult_message` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  consult_id BIGINT NOT NULL,
  sender_type VARCHAR(16) NOT NULL,
  sender_id BIGINT,
  sender_name VARCHAR(50),
  content VARCHAR(1000),
  create_time DATETIME,
  KEY idx_consult (consult_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_logistics` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(32),
  tracking_no VARCHAR(64),
  company VARCHAR(50),
  content VARCHAR(255),
  create_time DATETIME,
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_coupon` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_user_coupon` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  order_no VARCHAR(32),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 优惠券模板预置数据

INSERT INTO t_coupon (id,name,type,threshold,amount,total,received,limit_per_user,valid_days,status,description,create_time) VALUES
(1,'新人无门槛券',2,0.00,10.00,1000,0,1,30,1,'新用户专享，无门槛立减 10 元',CURRENT_TIMESTAMP),
(2,'满 99 减 15',1,99.00,15.00,500,0,2,30,1,'全场通用，满 99 元可用',CURRENT_TIMESTAMP),
(3,'满 199 减 30',1,199.00,30.00,300,0,1,30,1,'全场通用，满 199 元可用',CURRENT_TIMESTAMP),
(4,'满 299 减 50',1,299.00,50.00,200,0,1,30,1,'全场通用，满 299 元可用',CURRENT_TIMESTAMP);

-- 用户（密码加密规则：md5(明文 + LifeShop@2026)）

INSERT INTO t_user (id,username,password,nickname,phone,avatar,gender,role,status,create_time,update_time) VALUES
(1,'admin','52900efdfbfbbbadb254a39a7aad34a0','商城管理员','13800000000','',1,'ADMIN',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(2,'zhangsan','124421dbb5f14f4aec35f9927e135408','张三','13900000001','',1,'USER',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(3,'lisi','124421dbb5f14f4aec35f9927e135408','李四','13900000002','',0,'USER',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(4,'wangwu','124421dbb5f14f4aec35f9927e135408','王五','13900000003','',1,'USER',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(5,'zhaoliu','124421dbb5f14f4aec35f9927e135408','赵六','13900000004','',0,'USER',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(6,'shop_kitchen','124421dbb5f14f4aec35f9927e135408','陈明（悦家厨房旗舰店）','13700000001','',1,'MERCHANT',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(7,'shop_clean','124421dbb5f14f4aec35f9927e135408','刘芳（洁净家清专营店）','13700000002','',1,'MERCHANT',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(8,'shop_storage','124421dbb5f14f4aec35f9927e135408','周航（收纳研究所）','13700000003','',1,'MERCHANT',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(9,'shop_bath','124421dbb5f14f4aec35f9927e135408','吴静（暖居卫浴馆）','13700000004','',1,'MERCHANT',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(10,'shop_bedding','124421dbb5f14f4aec35f9927e135408','赵一鸣（眠光家纺）','13700000005','',1,'MERCHANT',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY)),
(11,'shop_daily','124421dbb5f14f4aec35f9927e135408','孙倩（日用百货铺）','13700000006','',1,'MERCHANT',1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_SUB(NOW(), INTERVAL 30 DAY));


-- 商品分类

INSERT INTO t_category (id,name,icon,sort,status,create_time) VALUES
(1,'厨房烹饪','🍳',1,1,DATE_SUB(NOW(), INTERVAL 30 DAY)),
(2,'清洁除菌','🧽',2,1,DATE_SUB(NOW(), INTERVAL 30 DAY)),
(3,'收纳整理','📦',3,1,DATE_SUB(NOW(), INTERVAL 30 DAY)),
(4,'卫浴洗漱','🛁',4,1,DATE_SUB(NOW(), INTERVAL 30 DAY)),
(5,'家纺床品','🛏️',5,1,DATE_SUB(NOW(), INTERVAL 30 DAY)),
(6,'家居日用','🏠',6,1,DATE_SUB(NOW(), INTERVAL 30 DAY));


-- 入驻商家（role = MERCHANT，登录后进入商家后台）

INSERT INTO t_merchant (id,user_id,name,logo,description,contact,phone,address,status,create_time,update_time) VALUES
(1,6,'悦家厨房旗舰店','','专注中式厨房十年，锅具、刀具、保鲜容器一站买齐。自有仓储，48 小时内发货。','陈明','13700000001','浙江省杭州市余杭区仓前街道文一西路 1218 号 3 号楼',1,DATE_SUB(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2,7,'洁净家清专营店','','做真正好用的清洁用品。油污、除菌、除螨，按场景给你配好方案。','刘芳','13700000002','广东省广州市白云区云城东路 88 号 A 栋',1,DATE_SUB(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3,8,'收纳研究所','','小户型收纳解决方案提供商。每一件产品都先在自己家里用满三个月再上架。','周航','13700000003','江苏省苏州市工业园区星湖街 328 号 6 号厂房',1,DATE_SUB(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4,9,'暖居卫浴馆','','卫浴五金与家纺洗护，主打恒温、增压、速干，让洗澡变成享受。','吴静','13700000004','福建省泉州市南安市仑苍镇卫浴产业园 12 号',1,DATE_SUB(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5,10,'眠光家纺','','来自家纺之都南通，纯棉四件套、枕头、凉席，只做 A 类母婴级面料。','赵一鸣','13700000005','江苏省南通市通州区川姜镇家纺城 B 区 15 号',1,DATE_SUB(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(6,11,'日用百货铺','','过日子要用的零碎小物，纸巾、雨伞、衣架、香薰，一件也能发。','孙倩','13700000006','浙江省义乌市国际商贸城二区 3 楼 18 街',1,DATE_SUB(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY));


-- 商品（主图为抓取到本地的真实商品图，merchant_id 标记所属店铺）

INSERT INTO t_product (id,merchant_id,category_id,name,subtitle,main_image,images,detail,price,original_price,stock,sales,unit,rating,review_count,status,create_time,update_time) VALUES
(1,1,1,'麦饭石不粘炒锅 30cm','少油不粘 · 燃气电磁炉通用 · 附赠木铲','/products/p1-1.jpg','/products/p1-1.jpg,/products/p1-2.jpg,/products/p1-3.jpg','锅体采用麦饭石复合涂层，煎蛋不放油也能整片揭起；加深 9cm 锅身，炒菜不易外溅。加厚锅底导热均匀，燃气灶、电磁炉通用。手柄为隔热防烫木纹设计，单手握持不打滑。',129.00,199.00,126,862,'口',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2,6,6,'316 不锈钢保温杯 500ml','24 小时锁温 · 内胆无缝 · 一键弹盖','/products/p2-1.jpg','/products/p2-1.jpg,/products/p2-2.jpg,/products/p2-3.jpg','316 不锈钢内胆，无焊接缝隙不易藏污。真空双层结构，95℃ 热水 12 小时后仍有 62℃。杯盖一键弹开，单手可开，通勤、带娃、开车都方便。',79.00,129.00,340,1245,'只',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3,1,1,'竹纤维洗碗布 5 片装','不沾油 · 速干不臭 · 可机洗','/products/p3-1.jpg','/products/p3-1.jpg,/products/p3-2.jpg,/products/p3-3.jpg','竹纤维材质，油污一冲即掉，不用洗洁精也能擦净轻度油渍。加厚双层织法，吸水后柔软有弹性，擦灶台、擦碗都不留水痕。速干不易滋生异味。',19.90,29.90,800,2310,'包',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4,1,1,'北欧陶瓷餐具 16 件套','釉下彩 · 可进微波炉洗碗机 · 4 人份','/products/p4-1.jpg','/products/p4-1.jpg,/products/p4-2.jpg,/products/p4-3.jpg','16 件包含 4 碗 4 盘 4 汤勺 4 筷架，刚好满足四口之家日常使用。釉下彩工艺，图案在釉层之下，不易磨损掉色。加厚瓷体，微波炉、烤箱、洗碗机均可使用。',238.00,358.00,88,431,'套',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5,2,2,'厨房重油污清洁剂 500ml','泡沫型 · 免刷洗 · 附赠喷头','/products/p5-1.png','/products/p5-1.png,/products/p5-2.jpg,/products/p5-3.jpg','喷出即膨胀成泡沫，附着在油烟机、灶台表面 3 分钟，油垢软化后用抹布一擦即净。配方温和不刺鼻，500ml 大容量，一瓶约可用 2 个月。',29.90,49.90,560,1876,'瓶',4.0,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6,1,1,'食品级硅胶铲 6 件套','不伤锅涂层 · 耐 230℃ · 带收纳桶','/products/p6-1.jpg','/products/p6-1.jpg,/products/p6-2.jpg,/products/p6-3.jpg','食品级硅胶包覆，边缘柔软，用在麦饭石、不粘锅上不划伤涂层。耐温 -40℃ 至 230℃，炒菜、烘焙都能用。一体成型无缝隙，附赠沥水收纳桶。',59.00,89.00,210,654,'套',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(19,1,1,'多功能厨房剪刀','带磁吸刀套 · 剪骨不费力 · 可拆洗','/products/p19-1.jpg','/products/p19-1.jpg,/products/p19-2.jpg,/products/p19-3.jpg','加厚不锈钢刀刃，剪鸡骨、剪葱花都利落，中间带凹槽可夹核桃。手柄防滑贴合，右手左手都好握；刀身可拆开单独清洗，避免缝隙藏污。',39.00,59.00,260,742,'把',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(20,1,1,'天然竹砧板 大号','双面可用 · 带沥水槽 · 不易发霉','/products/p20-1.jpg','/products/p20-1.jpg,/products/p20-2.jpg,/products/p20-3.jpg','整竹拼接，密度高不易吸水，南方潮湿环境也相对不容易发霉。一面切菜一面切水果，边缘有导流槽，切肉汁水不会流到台面。',69.00,99.00,180,1305,'块',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(21,1,1,'玻璃保鲜盒 4 件套','可微波可冷冻 · 密封扣盖 · 可叠放','/products/p21-1.jpg','/products/p21-1.jpg,/products/p21-2.jpg,/products/p21-3.jpg','高硼硅玻璃，耐 -20℃ 至 400℃，冰箱拿出来可以直接进微波炉。四扣密封盖，汤汁不侧漏，装便当带通勤也不怕洒。四种尺寸可叠放，冰箱更整齐。',55.00,89.00,320,892,'套',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(22,1,1,'双层不锈钢蒸锅 28cm','一锅多用 · 燃气电磁炉通用 · 可视锅盖','/products/p22-1.jpg','/products/p22-1.jpg,/products/p22-2.jpg,/products/p22-3.jpg','304 不锈钢锅身，双层设计上蒸下煮，早餐一锅搞定包子加鸡蛋。加厚复合锅底，受热均匀不糊底，燃气灶、电磁炉都能用。',149.00,229.00,90,468,'口',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7,2,2,'免手洗平板拖把套装','单手刮水 · 干湿两用 · 含 2 块拖布','/products/p7-1.jpg','/products/p7-1.jpg,/products/p7-2.jpg,/products/p7-3.jpg','推拉式刮水结构，拖完按一下就能把脏水刮进桶里，全程不沾手。平板拖头 360° 旋转，沙发底、床底都能探进去。附赠 2 块超细纤维拖布。',89.00,139.00,175,942,'套',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8,2,2,'洗衣机槽清洁泡腾片 12 片','除垢去味 · 滚筒波轮通用 · 一片一次','/products/p8-1.jpg','/products/p8-1.jpg,/products/p8-2.jpg,/products/p8-3.jpg','一片一次，丢进空桶选标准洗即可。泡腾配方深入内筒夹层，分解洗衣液残留与水垢，减少异味来源。滚筒、波轮洗衣机通用。',25.00,39.00,620,1503,'盒',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(23,2,2,'洗衣凝珠 50 颗','深层去渍 · 持久留香 · 三合一','/products/p23-1.png','/products/p23-1.png,/products/p23-2.jpg,/products/p23-3.png','一颗搞定洗净、柔顺、留香，不用再单独倒柔顺剂。水溶膜遇水即化，冷水也能完全溶解不残留。50 颗装约可用两个月。',45.00,79.00,500,2103,'盒',4.0,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(24,2,2,'加厚手提垃圾袋 100 只','自动收口 · 加厚不漏 · 家用大号','/products/p24-1.jpg','/products/p24-1.jpg,/products/p24-2.jpg,/products/p24-3.jpg','加厚 HDPE 材质，装满厨余也不易破底。手提抽绳设计，一拉即收口，拎下楼不脏手。100 只分 5 卷，断点清晰好撕。',19.90,32.00,900,3420,'卷',5.0,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(25,2,2,'75% 酒精消毒湿巾 80 抽','家用办公 · 不伤手 · 抽取式','/products/p25-1.jpg','/products/p25-1.jpg,/products/p25-2.jpg,/products/p25-3.jpg','75% 酒精配比，擦门把、擦手机、擦办公桌都合适。加厚无纺布不易破，含水量足，抽出来不干。含保湿成分，频繁使用手也不紧绷。',22.00,35.00,600,1877,'包',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(26,2,2,'磁吸双面玻璃刮水器','高层适用 · 双面同步擦 · 可拆洗','/products/p26-1.jpg','/products/p26-1.jpg,/products/p26-2.jpg,/products/p26-3.jpg','内外磁吸同步移动，站在屋里就能擦到窗外那面。刮条贴合玻璃，一遍刮过不留水痕。配安全绳，高层使用更放心。',49.00,79.00,260,634,'个',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(27,2,2,'除螨喷雾 500ml','免洗 · 床品沙发可用 · 淡淡清香','/products/p27-1.jpg','/products/p27-1.jpg,/products/p27-2.jpg,/products/p27-3.jpg','喷在床垫、沙发、地毯表面，静置晾干即可，不用再洗一遍。适合换季、梅雨季，家里有宠物也可以定期用。气味清淡不刺激。',35.00,59.00,420,1052,'瓶',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9,3,3,'无痕强力粘钩 20 枚','承重 5kg · 免打孔 · 撕下不留胶','/products/p9-1.jpg','/products/p9-1.jpg,/products/p9-2.jpg,/products/p9-3.jpg','加厚背胶，贴墙 24 小时后承重可达 5kg，挂锅铲、浴球、钥匙都稳。撕下时用吹风机热风吹 30 秒，可整片揭下不留胶痕，租房友好。',15.90,25.90,900,2765,'盒',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(12,3,3,'真空压缩收纳袋 8 件套','含手泵 · 节省 70% 空间 · 防潮防虫','/products/p12-1.jpg','/products/p12-1.jpg,/products/p12-2.jpg,/products/p12-3.jpg','8 件套含 3 大 3 中 2 小，换季被褥、羽绒服都能压。双轨密封条 + 防漏气阀，压好后可维持数月不进气。附赠手泵，出差、露营也能用。',40.00,69.00,380,1102,'套',5.0,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(28,3,3,'抽屉分隔收纳盒 6 件套','自由组合 · 桌面衣柜通用 · 透明可视','/products/p28-1.jpg','/products/p28-1.jpg,/products/p28-2.jpg,/products/p28-3.jpg','六种尺寸可自由拼进抽屉，袜子、内衣、文具各归各位。透明盒身一眼看到内容物，不用翻箱倒柜。边缘圆润，抽拉不刮手。',32.00,49.00,480,1560,'套',3.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(29,3,3,'免钉多层鞋架 4 层','承重 20kg · 门口窄位可用 · 可叠高','/products/p29-1.jpg','/products/p29-1.jpg,/products/p29-2.jpg,/products/p29-3.jpg','免工具组装，卡扣一按就成型。4 层可放 12 双常穿鞋，层高适配运动鞋与短靴。窄身设计，门后、玄关角落都塞得下。',79.00,129.00,210,720,'个',3.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(30,3,3,'冰箱收纳保鲜盒 5 件套','透明可视 · 可叠放 · 冰箱更整齐','/products/p30-1.jpg','/products/p30-1.jpg,/products/p30-2.jpg,/products/p30-3.jpg','五种尺寸对应水果、鸡蛋、剩菜、调料，冰箱分层不再乱。透明盒身加手柄，拿出来一眼看清。可叠放，纵向空间也利用起来。',45.00,69.00,340,981,'套',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(31,3,3,'可折叠洗衣篮','大容量 · 手提防滑 · 不用时折起来','/products/p31-1.jpg','/products/p31-1.jpg,/products/p31-2.jpg,/products/p31-3.jpg','70L 大容量，一家三口一周的换洗衣服装得下。两侧提手加厚，装满拎着不勒手。不用时压扁收进柜缝，卫生间不占地。',39.00,59.00,300,655,'个',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(32,3,3,'三层置物推车','带刹车轮 · 厨房浴室通用 · 承重 15kg','/products/p32-1.jpg','/products/p32-1.jpg,/products/p32-2.jpg,/products/p32-3.jpg','三层大容量，厨房放调料、浴室放洗护、书房放文具都合适。万向轮带刹车，推到哪停到哪。镂空篮身透气，湿区用也不积水。',119.00,189.00,150,508,'台',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(33,3,3,'挂式真空压缩袋 5 件套','直接挂衣柜 · 免抽气 · 防尘防潮','/products/p33-1.jpg','/products/p33-1.jpg,/products/p33-2.jpg,/products/p33-3.jpg','压好之后可以直接挂在衣柜杆上，羽绒服、大衣一件一袋。免抽气设计，卷压排气就行，出差酒店也能用。透明视窗，找衣服不用全拆。',59.00,89.00,260,433,'套',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10,4,4,'一次性洗脸巾 80 抽','纯棉加厚 · 干湿两用 · 抽取式','/products/p10-1.jpg','/products/p10-1.jpg,/products/p10-2.jpg,/products/p10-3.jpg','100% 纯棉水刺无纺布，厚度比普通洗脸巾高 30%，湿水后不易破。珍珠纹面用于清洁，平纹面用于擦干，一片可替代毛巾使用。',24.90,39.90,700,1988,'包',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13,4,4,'纯棉毛巾 3 条装','32 支长绒棉 · 吸水速干 · 亲肤不掉毛','/products/p13-1.jpg','/products/p13-1.jpg,/products/p13-2.jpg,/products/p13-3.jpg','32 支长绒棉，克重 420g/㎡，摸起来厚实柔软。毛圈密度高，吸水快也干得快，浴室挂一晚基本干透。3 条装可区分洗脸、洗澡、擦手。',45.00,69.00,520,1674,'组',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(14,4,4,'纯棉加厚浴巾 70x140','吸水快干 · 可裹全身 · 亲肤柔软','/products/p14-1.jpg','/products/p14-1.jpg,/products/p14-2.jpg,/products/p14-3.jpg','70×140cm 加大尺寸，成年人可裹住全身。加厚毛圈吸水量约为自重的 4 倍，洗完澡一裹即干。多次水洗后仍保持柔软，不易变硬。',59.90,89.00,300,786,'条',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(15,4,4,'浴室防滑拖鞋','EVA 一体成型 · 防滑底纹 · 速干不臭','/products/p15-1.jpg','/products/p15-1.jpg,/products/p15-2.jpg,/products/p15-3.jpg','EVA 一体成型，无缝隙不藏水，洗完澡踩一会儿就干。鞋底深纹路抓地，湿瓷砖上也不打滑。轻至 180g，久穿不累脚。',35.00,59.00,450,1398,'双',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(16,4,4,'陶瓷漱口杯 2 只装','简约哑光 · 可放牙刷 · 易清洗','/products/p16-1.jpg','/products/p16-1.jpg,/products/p16-2.jpg,/products/p16-3.jpg','哑光釉面手感细腻，杯口圆润不割嘴。杯身高度可放常规牙刷，一家两口各一只不混用。瓷质厚实，日常使用很稳。',26.00,39.00,260,523,'组',3.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(34,4,4,'增压淋浴花洒','三档调节 · 一键止水 · 易安装','/products/p34-1.jpg','/products/p34-1.jpg,/products/p34-2.jpg,/products/p34-3.jpg','细密出水孔增压，高层水压小也能洗得舒服。三档模式切换，按摩档冲肩颈很解乏。手柄一键止水，抹沐浴露时不用反复开关水阀。',79.00,129.00,280,1129,'个',3.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(35,4,4,'浴室置物架 免打孔','双层太空铝 · 防锈 · 承重 10kg','/products/p35-1.jpg','/products/p35-1.jpg,/products/p35-2.jpg,/products/p35-3.jpg','免打孔背胶安装，租房也能放心用。太空铝材质不生锈，长期潮气环境也不掉漆。双层加护栏，洗发水沐浴露摆满不滑落。',69.00,109.00,240,830,'个',3.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(36,4,4,'硅藻泥吸水地垫','速干 · 浴室门口 · 不打滑','/products/p36-1.jpg','/products/p36-1.jpg,/products/p36-2.jpg,/products/p36-3.jpg','踩上去几秒吸干脚底水，浴室门口不再一片湿。表面磨砂防滑，湿脚踩也不打滑。脏了用清水冲一冲，晾干继续用。',45.00,75.00,360,1244,'块',5.0,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(17,5,5,'天然乳胶枕 枕芯','93% 乳胶含量 · 透气孔设计 · 护颈曲线','/products/p17-1.jpg','/products/p17-1.jpg,/products/p17-2.jpg,/products/p17-3.jpg','93% 天然乳胶含量，回弹快、支撑足，睡醒不易塌。表面密集透气孔，夏天不闷汗。波浪曲线设计贴合颈部，仰睡侧睡都有支撑。',159.00,259.00,140,467,'只',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(18,5,5,'全棉四件套 1.8m 床','60 支长绒棉 · 亲肤透气 · 四季可用','/products/p18-1.jpg','/products/p18-1.jpg,/products/p18-2.jpg,/products/p18-3.png','60 支长绒棉，密度 133×72，摸起来细腻不扎。活性印染，色牢度高，多次水洗不易褪色。含被套 1、床单 1、枕套 2，适配 1.8m 床。',269.00,399.00,96,312,'套',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(37,5,5,'水洗棉夏凉被','可机洗 · 空调房适用 · 亲肤不闷','/products/p37-1.jpg','/products/p37-1.jpg,/products/p37-2.jpg,/products/p37-3.jpg','水洗棉面料，柔软带一点自然的褶皱感，直接盖不扎人。填充轻薄，空调房 26℃ 盖着刚好不闷汗。整条可机洗，晾干就能用。',129.00,199.00,180,690,'床',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(38,5,5,'床垫保护垫','防水防螨 · 可拆洗 · 四周松紧带','/products/p38-1.jpg','/products/p38-1.jpg,/products/p38-2.jpg,/products/p38-3.jpg','表层纯棉亲肤，中间防水膜挡住汗渍、尿渍，床垫用几年还干净。四周松紧带包边，翻身不跑位。可整条机洗，家里有小孩宠物很实用。',99.00,159.00,220,470,'条',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(39,5,5,'抗菌枕巾 2 条装','纯棉 · 吸汗透气 · 换洗方便','/products/p39-1.jpg','/products/p39-1.jpg,/products/p39-2.jpg,/products/p39-3.jpg','枕巾比换枕套省事，出汗多的人建议一周一换。纯棉材质吸汗透气，抗菌处理减少异味。2 条装方便替换。',35.00,55.00,400,866,'组',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(40,5,5,'针织沙发盖毯','130x180 · 四季可用 · 可机洗','/products/p40-1.jpg','/products/p40-1.jpg,/products/p40-2.jpg,/products/p40-3.jpg','针织纹理有质感，搭在沙发上就是装饰，追剧时随手一盖。130×180 够两个人分。可机洗，不起球不缩水。',89.00,139.00,200,545,'条',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(41,5,5,'遮光窗帘 2 片装','隔热降噪 · 遮光率 90% · 免打孔可选','/products/p41-1.jpg','/products/p41-1.jpg,/products/p41-2.jpg,/products/p41-3.jpg','三层复合织造，遮光率约 90%，上夜班白天补觉也不刺眼。布身有垂感，挂起来褶皱好看。可选免打孔伸缩杆安装，租房友好。',159.00,259.00,160,388,'套',3.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(42,5,5,'冰丝凉席三件套','可折叠 · 亲肤凉爽 · 含枕套 2 只','/products/p42-1.jpg','/products/p42-1.jpg,/products/p42-2.jpg,/products/p42-3.jpg','冰丝纤维贴身凉爽但不冰人，比竹席更适合空调房。可折叠收纳，换季塞进柜子不占地。含凉席 1 张、枕套 2 只。',139.00,219.00,140,402,'套',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(11,6,6,'维达抽纸 24 包整箱','3 层加厚 · 超韧不易破 · 家庭装','/products/p11-1.jpg','/products/p11-1.jpg,/products/p11-2.jpg,/products/p11-3.jpg','3 层加厚压花，湿水后仍不易破，擦手擦桌都够用。24 包整箱家庭装，客厅、卧室、车里各放一包。原生木浆，无荧光增白剂。',49.90,79.90,460,3120,'箱',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(43,6,6,'全自动晴雨伞','十骨加固 · 防紫外线 · 一按即开','/products/p43-1.jpg','/products/p43-1.jpg,/products/p43-2.jpg,/products/p43-3.jpg','十骨加固伞架，大风天不容易翻伞。伞布带黑胶涂层，晴天遮阳防紫外线。一键自动开收，上车、抱娃时单手就能操作。',59.00,99.00,350,1420,'把',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(44,6,6,'无火香薰 200ml','客厅卧室 · 持久扩香 · 含藤条','/products/p44-1.jpg','/products/p44-1.jpg,/products/p44-2.jpg,/products/p44-3.jpg','藤条自然挥发，不用点火也不用插电，家里有小孩宠物更安心。200ml 约可散香 2-3 个月，玄关、卫生间放一瓶进门就有好味道。',69.00,109.00,300,977,'瓶',4.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(45,6,6,'桌面加湿器','静音 · USB 供电 · 带小夜灯','/products/p45-1.jpg','/products/p45-1.jpg,/products/p45-2.jpg,/products/p45-3.jpg','超声波雾化，出雾细腻不积水珠，办公桌、床头柜都合适。运行声音很轻，睡觉开着不吵。带暖光小夜灯，起夜不用摸黑开大灯。',59.00,99.00,280,1130,'台',4.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(46,6,6,'防滑衣架 20 个装','无痕 · 干湿两用 · 承重 5kg','/products/p46-1.jpg','/products/p46-1.jpg,/products/p46-2.jpg,/products/p46-3.jpg','肩部带防滑齿，针织衫、真丝裙挂上去不往下滑。加厚材质，冬天挂大衣也不变形。干湿两用，阳台晾完直接挂回衣柜。',25.00,39.00,700,2210,'组',3.5,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(47,6,6,'除湿盒 6 盒装','衣柜鞋柜 · 可重复使用 · 无香味','/products/p47-1.jpg','/products/p47-1.jpg,/products/p47-2.jpg,/products/p47-3.jpg','氯化钙吸湿，梅雨季衣柜、鞋柜放一盒，衣服不再潮乎乎。吸满水后倒掉，晒干或微波加热可重复使用。无香型，不串味。',29.00,45.00,520,1655,'组',4.0,2,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(48,6,6,'静音挂钟 12 英寸','简约北欧 · 扫描机芯 · 夜间不吵','/products/p48-1.jpg','/products/p48-1.jpg,/products/p48-2.jpg,/products/p48-3.jpg','扫描机芯无滴答声，卧室、书房挂着不扰人。12 英寸表盘数字清晰，客厅沙发上看时间也够大。哑光边框，配原木、奶油风都好看。',69.00,109.00,190,512,'个',5.0,1,1,DATE_SUB(NOW(), INTERVAL 28 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY));


-- 首页轮播

INSERT INTO t_banner (id,title,image,link,sort,status,create_time) VALUES
(1,'厨房焕新季 · 好锅把家常菜做得更好吃','/banners/b1.svg','/list?categoryId=1',1,1,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2,'干净的家 · 每周 20 分钟清洁除菌','/banners/b2.svg','/list?categoryId=2',2,1,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3,'小家大收纳 · 换季被褥省 70% 空间','/banners/b3.svg','/list?categoryId=3',3,1,DATE_SUB(NOW(), INTERVAL 20 DAY));


-- 收货地址

INSERT INTO t_address (id,user_id,receiver,phone,province,city,district,detail,is_default,create_time) VALUES
(1,2,'张三','13900000001','浙江省','杭州市','西湖区','文三路 100 号 3 幢 502 室',1,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2,2,'张三（公司）','13900000001','浙江省','杭州市','滨江区','江南大道 588 号 A 座 12 楼',0,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3,3,'李四','13900000002','江苏省','南京市','鼓楼区','中山北路 66 号 2 单元 801',1,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4,4,'王五','13900000003','广东省','深圳市','南山区','科技园南路 18 号 5 栋 903',1,DATE_SUB(NOW(), INTERVAL 20 DAY)),
(5,5,'赵六','13900000004','四川省','成都市','武侯区','天府大道中段 199 号 2 单元 601',1,DATE_SUB(NOW(), INTERVAL 20 DAY));


-- 购物车（演示用户 zhangsan 有 3 件商品）

INSERT INTO t_cart_item (id,user_id,product_id,quantity,checked,create_time) VALUES
(1,2,2,1,1,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2,2,5,2,1,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3,2,9,1,1,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4,3,12,1,1,DATE_SUB(NOW(), INTERVAL 1 DAY));


-- 订单（按店铺拆单，覆盖待付款/待发货/已发货/已完成四种状态）

INSERT INTO t_order (id,order_no,user_id,merchant_id,merchant_name,total_amount,freight,pay_amount,status,receiver,phone,address,remark,pay_time,ship_time,finish_time,create_time,update_time) VALUES
(1,'LS2026100110001001',2,1,'悦家厨房旗舰店',129.00,0.00,129.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2,'LS2026100210001002',2,4,'暖居卫浴馆',59.90,10.00,69.90,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3,'LS2026100310001003',2,6,'日用百货铺',49.90,10.00,59.90,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4,'LS2026100410001004',2,5,'眠光家纺',159.00,0.00,159.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5,'LS2026100510001005',2,2,'洁净家清专营店',89.00,10.00,99.00,2,'张三（公司）','13900000001','浙江省杭州市滨江区江南大道 588 号 A 座 12 楼',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6,'LS2026100610001006',2,3,'收纳研究所',40.00,10.00,50.00,2,'张三（公司）','13900000001','浙江省杭州市滨江区江南大道 588 号 A 座 12 楼',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7,'LS2026100710001007',2,2,'洁净家清专营店',84.80,10.00,94.80,0,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室','请放门口，谢谢',NULL,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8,'LS2026100810001008',3,1,'悦家厨房旗舰店',238.00,0.00,238.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(9,'LS2026100910001009',3,6,'日用百货铺',79.00,10.00,89.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10,'LS2026101010001010',4,5,'眠光家纺',269.00,0.00,269.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(11,'LS2026101110001011',4,4,'暖居卫浴馆',75.80,10.00,85.80,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(12,'LS2026101210001012',5,4,'暖居卫浴馆',80.00,10.00,90.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(13,'LS2026101310001013',3,1,'悦家厨房旗舰店',39.80,10.00,49.80,1,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,NOW(),NULL,NULL,NOW(),NOW()),
(14,'LS2026101410001014',3,3,'收纳研究所',15.90,10.00,25.90,1,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,NOW(),NULL,NULL,NOW(),NOW()),
(15,'LS2026101510001015',5,1,'悦家厨房旗舰店',59.00,10.00,69.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(16,'LS2026101610001016',2,1,'悦家厨房旗舰店',69.00,10.00,79.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17,'LS2026101710001017',2,2,'洁净家清专营店',19.90,10.00,29.90,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(18,'LS2026101810001018',5,1,'悦家厨房旗舰店',19.90,10.00,29.90,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(19,'LS2026101910001019',2,1,'悦家厨房旗舰店',19.90,10.00,29.90,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(20,'LS2026102010001020',3,2,'洁净家清专营店',29.90,10.00,39.90,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(21,'LS2026102110001021',4,2,'洁净家清专营店',29.90,10.00,39.90,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(22,'LS2026102210001022',5,1,'悦家厨房旗舰店',39.00,10.00,49.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(23,'LS2026102310001023',2,1,'悦家厨房旗舰店',69.00,10.00,79.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(24,'LS2026102410001024',3,1,'悦家厨房旗舰店',69.00,10.00,79.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(25,'LS2026102510001025',3,1,'悦家厨房旗舰店',55.00,10.00,65.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(26,'LS2026102610001026',4,1,'悦家厨房旗舰店',149.00,0.00,149.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(27,'LS2026102710001027',5,2,'洁净家清专营店',89.00,10.00,99.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(28,'LS2026102810001028',2,2,'洁净家清专营店',25.00,10.00,35.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(29,'LS2026102910001029',3,2,'洁净家清专营店',25.00,10.00,35.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(30,'LS2026103010001030',5,2,'洁净家清专营店',45.00,10.00,55.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(31,'LS2026103110001031',2,2,'洁净家清专营店',45.00,10.00,55.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(32,'LS2026103210001032',2,2,'洁净家清专营店',19.90,10.00,29.90,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(33,'LS2026103310001033',3,2,'洁净家清专营店',19.90,10.00,29.90,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(34,'LS2026103410001034',3,2,'洁净家清专营店',22.00,10.00,32.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(35,'LS2026103510001035',4,2,'洁净家清专营店',22.00,10.00,32.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(36,'LS2026103610001036',4,2,'洁净家清专营店',49.00,10.00,59.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(37,'LS2026103710001037',5,2,'洁净家清专营店',35.00,10.00,45.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(38,'LS2026103810001038',2,2,'洁净家清专营店',35.00,10.00,45.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(39,'LS2026103910001039',3,3,'收纳研究所',15.90,10.00,25.90,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(40,'LS2026104010001040',4,3,'收纳研究所',15.90,10.00,25.90,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(41,'LS2026104110001041',2,3,'收纳研究所',40.00,10.00,50.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(42,'LS2026104210001042',3,3,'收纳研究所',40.00,10.00,50.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(43,'LS2026104310001043',2,3,'收纳研究所',32.00,10.00,42.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(44,'LS2026104410001044',3,3,'收纳研究所',32.00,10.00,42.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(45,'LS2026104510001045',3,3,'收纳研究所',79.00,10.00,89.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(46,'LS2026104610001046',4,3,'收纳研究所',45.00,10.00,55.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(47,'LS2026104710001047',5,3,'收纳研究所',39.00,10.00,49.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(48,'LS2026104810001048',2,3,'收纳研究所',119.00,0.00,119.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(49,'LS2026104910001049',3,3,'收纳研究所',59.00,10.00,69.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(50,'LS2026105010001050',4,4,'暖居卫浴馆',79.00,10.00,89.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(51,'LS2026105110001051',5,4,'暖居卫浴馆',79.00,10.00,89.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(52,'LS2026105210001052',5,4,'暖居卫浴馆',69.00,10.00,79.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(53,'LS2026105310001053',2,4,'暖居卫浴馆',45.00,10.00,55.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(54,'LS2026105410001054',3,4,'暖居卫浴馆',45.00,10.00,55.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(55,'LS2026105510001055',3,5,'眠光家纺',129.00,0.00,129.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(56,'LS2026105610001056',4,5,'眠光家纺',99.00,0.00,99.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(57,'LS2026105710001057',5,5,'眠光家纺',35.00,10.00,45.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(58,'LS2026105810001058',2,5,'眠光家纺',89.00,10.00,99.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(59,'LS2026105910001059',3,5,'眠光家纺',159.00,0.00,159.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(60,'LS2026106010001060',4,5,'眠光家纺',139.00,0.00,139.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(61,'LS2026106110001061',5,6,'日用百货铺',59.00,10.00,69.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(62,'LS2026106210001062',2,6,'日用百货铺',59.00,10.00,69.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(63,'LS2026106310001063',2,6,'日用百货铺',69.00,10.00,79.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(64,'LS2026106410001064',3,6,'日用百货铺',59.00,10.00,69.00,3,'李四','13900000002','江苏省南京市鼓楼区中山北路 66 号 2 单元 801',NULL,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(65,'LS2026106510001065',4,6,'日用百货铺',59.00,10.00,69.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(66,'LS2026106610001066',4,6,'日用百货铺',25.00,10.00,35.00,3,'王五','13900000003','广东省深圳市南山区科技园南路 18 号 5 栋 903',NULL,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(67,'LS2026106710001067',5,6,'日用百货铺',25.00,10.00,35.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(68,'LS2026106810001068',5,6,'日用百货铺',29.00,10.00,39.00,3,'赵六','13900000004','四川省成都市武侯区天府大道中段 199 号 2 单元 601',NULL,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(69,'LS2026106910001069',2,6,'日用百货铺',29.00,10.00,39.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(70,'LS2026107010001070',2,6,'日用百货铺',69.00,10.00,79.00,3,'张三','13900000001','浙江省杭州市西湖区文三路 100 号 3 幢 502 室',NULL,DATE_SUB(NOW(), INTERVAL 1 DAY),NOW(),NOW(),DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY));


-- 订单明细（保存下单时的商品快照）

INSERT INTO t_order_item (id,order_id,order_no,merchant_id,product_id,product_name,product_image,price,quantity,total_price) VALUES
(1,1,'LS2026100110001001',1,1,'麦饭石不粘炒锅 30cm','/products/p1-1.jpg',129.00,1,129.00),
(2,2,'LS2026100210001002',4,14,'纯棉加厚浴巾 70x140','/products/p14-1.jpg',59.90,1,59.90),
(3,3,'LS2026100310001003',6,11,'维达抽纸 24 包整箱','/products/p11-1.jpg',49.90,1,49.90),
(4,4,'LS2026100410001004',5,17,'天然乳胶枕 枕芯','/products/p17-1.jpg',159.00,1,159.00),
(5,5,'LS2026100510001005',2,7,'免手洗平板拖把套装','/products/p7-1.jpg',89.00,1,89.00),
(6,6,'LS2026100610001006',3,12,'真空压缩收纳袋 8 件套','/products/p12-1.jpg',40.00,1,40.00),
(7,7,'LS2026100710001007',2,5,'厨房重油污清洁剂 500ml','/products/p5-1.png',29.90,2,59.80),
(8,7,'LS2026100710001007',2,8,'洗衣机槽清洁泡腾片 12 片','/products/p8-1.jpg',25.00,1,25.00),
(9,8,'LS2026100810001008',1,4,'北欧陶瓷餐具 16 件套','/products/p4-1.jpg',238.00,1,238.00),
(10,9,'LS2026100910001009',6,2,'316 不锈钢保温杯 500ml','/products/p2-1.jpg',79.00,1,79.00),
(11,10,'LS2026101010001010',5,18,'全棉四件套 1.8m 床','/products/p18-1.jpg',269.00,1,269.00),
(12,11,'LS2026101110001011',4,16,'陶瓷漱口杯 2 只装','/products/p16-1.jpg',26.00,1,26.00),
(13,11,'LS2026101110001011',4,10,'一次性洗脸巾 80 抽','/products/p10-1.jpg',24.90,2,49.80),
(14,12,'LS2026101210001012',4,13,'纯棉毛巾 3 条装','/products/p13-1.jpg',45.00,1,45.00),
(15,12,'LS2026101210001012',4,15,'浴室防滑拖鞋','/products/p15-1.jpg',35.00,1,35.00),
(16,13,'LS2026101310001013',1,3,'竹纤维洗碗布 5 片装','/products/p3-1.jpg',19.90,2,39.80),
(17,14,'LS2026101410001014',3,9,'无痕强力粘钩 20 枚','/products/p9-1.jpg',15.90,1,15.90),
(18,15,'LS2026101510001015',1,6,'食品级硅胶铲 6 件套','/products/p6-1.jpg',59.00,1,59.00),
(19,16,'LS2026101610001016',1,20,'天然竹砧板 大号','/products/p20-1.jpg',69.00,1,69.00),
(20,17,'LS2026101710001017',2,24,'加厚手提垃圾袋 100 只','/products/p24-1.jpg',19.90,1,19.90),
(21,18,'LS2026101810001018',1,3,'竹纤维洗碗布 5 片装','/products/p3-1.jpg',19.90,1,19.90),
(22,19,'LS2026101910001019',1,3,'竹纤维洗碗布 5 片装','/products/p3-1.jpg',19.90,1,19.90),
(23,20,'LS2026102010001020',2,5,'厨房重油污清洁剂 500ml','/products/p5-1.png',29.90,1,29.90),
(24,21,'LS2026102110001021',2,5,'厨房重油污清洁剂 500ml','/products/p5-1.png',29.90,1,29.90),
(25,22,'LS2026102210001022',1,19,'多功能厨房剪刀','/products/p19-1.jpg',39.00,1,39.00),
(26,23,'LS2026102310001023',1,20,'天然竹砧板 大号','/products/p20-1.jpg',69.00,1,69.00),
(27,24,'LS2026102410001024',1,20,'天然竹砧板 大号','/products/p20-1.jpg',69.00,1,69.00),
(28,25,'LS2026102510001025',1,21,'玻璃保鲜盒 4 件套','/products/p21-1.jpg',55.00,1,55.00),
(29,26,'LS2026102610001026',1,22,'双层不锈钢蒸锅 28cm','/products/p22-1.jpg',149.00,1,149.00),
(30,27,'LS2026102710001027',2,7,'免手洗平板拖把套装','/products/p7-1.jpg',89.00,1,89.00),
(31,28,'LS2026102810001028',2,8,'洗衣机槽清洁泡腾片 12 片','/products/p8-1.jpg',25.00,1,25.00),
(32,29,'LS2026102910001029',2,8,'洗衣机槽清洁泡腾片 12 片','/products/p8-1.jpg',25.00,1,25.00),
(33,30,'LS2026103010001030',2,23,'洗衣凝珠 50 颗','/products/p23-1.png',45.00,1,45.00),
(34,31,'LS2026103110001031',2,23,'洗衣凝珠 50 颗','/products/p23-1.png',45.00,1,45.00),
(35,32,'LS2026103210001032',2,24,'加厚手提垃圾袋 100 只','/products/p24-1.jpg',19.90,1,19.90),
(36,33,'LS2026103310001033',2,24,'加厚手提垃圾袋 100 只','/products/p24-1.jpg',19.90,1,19.90),
(37,34,'LS2026103410001034',2,25,'75% 酒精消毒湿巾 80 抽','/products/p25-1.jpg',22.00,1,22.00),
(38,35,'LS2026103510001035',2,25,'75% 酒精消毒湿巾 80 抽','/products/p25-1.jpg',22.00,1,22.00),
(39,36,'LS2026103610001036',2,26,'磁吸双面玻璃刮水器','/products/p26-1.jpg',49.00,1,49.00),
(40,37,'LS2026103710001037',2,27,'除螨喷雾 500ml','/products/p27-1.jpg',35.00,1,35.00),
(41,38,'LS2026103810001038',2,27,'除螨喷雾 500ml','/products/p27-1.jpg',35.00,1,35.00),
(42,39,'LS2026103910001039',3,9,'无痕强力粘钩 20 枚','/products/p9-1.jpg',15.90,1,15.90),
(43,40,'LS2026104010001040',3,9,'无痕强力粘钩 20 枚','/products/p9-1.jpg',15.90,1,15.90),
(44,41,'LS2026104110001041',3,12,'真空压缩收纳袋 8 件套','/products/p12-1.jpg',40.00,1,40.00),
(45,42,'LS2026104210001042',3,12,'真空压缩收纳袋 8 件套','/products/p12-1.jpg',40.00,1,40.00),
(46,43,'LS2026104310001043',3,28,'抽屉分隔收纳盒 6 件套','/products/p28-1.jpg',32.00,1,32.00),
(47,44,'LS2026104410001044',3,28,'抽屉分隔收纳盒 6 件套','/products/p28-1.jpg',32.00,1,32.00),
(48,45,'LS2026104510001045',3,29,'免钉多层鞋架 4 层','/products/p29-1.jpg',79.00,1,79.00),
(49,46,'LS2026104610001046',3,30,'冰箱收纳保鲜盒 5 件套','/products/p30-1.jpg',45.00,1,45.00),
(50,47,'LS2026104710001047',3,31,'可折叠洗衣篮','/products/p31-1.jpg',39.00,1,39.00),
(51,48,'LS2026104810001048',3,32,'三层置物推车','/products/p32-1.jpg',119.00,1,119.00),
(52,49,'LS2026104910001049',3,33,'挂式真空压缩袋 5 件套','/products/p33-1.jpg',59.00,1,59.00),
(53,50,'LS2026105010001050',4,34,'增压淋浴花洒','/products/p34-1.jpg',79.00,1,79.00),
(54,51,'LS2026105110001051',4,34,'增压淋浴花洒','/products/p34-1.jpg',79.00,1,79.00),
(55,52,'LS2026105210001052',4,35,'浴室置物架 免打孔','/products/p35-1.jpg',69.00,1,69.00),
(56,53,'LS2026105310001053',4,36,'硅藻泥吸水地垫','/products/p36-1.jpg',45.00,1,45.00),
(57,54,'LS2026105410001054',4,36,'硅藻泥吸水地垫','/products/p36-1.jpg',45.00,1,45.00),
(58,55,'LS2026105510001055',5,37,'水洗棉夏凉被','/products/p37-1.jpg',129.00,1,129.00),
(59,56,'LS2026105610001056',5,38,'床垫保护垫','/products/p38-1.jpg',99.00,1,99.00),
(60,57,'LS2026105710001057',5,39,'抗菌枕巾 2 条装','/products/p39-1.jpg',35.00,1,35.00),
(61,58,'LS2026105810001058',5,40,'针织沙发盖毯','/products/p40-1.jpg',89.00,1,89.00),
(62,59,'LS2026105910001059',5,41,'遮光窗帘 2 片装','/products/p41-1.jpg',159.00,1,159.00),
(63,60,'LS2026106010001060',5,42,'冰丝凉席三件套','/products/p42-1.jpg',139.00,1,139.00),
(64,61,'LS2026106110001061',6,43,'全自动晴雨伞','/products/p43-1.jpg',59.00,1,59.00),
(65,62,'LS2026106210001062',6,43,'全自动晴雨伞','/products/p43-1.jpg',59.00,1,59.00),
(66,63,'LS2026106310001063',6,44,'无火香薰 200ml','/products/p44-1.jpg',69.00,1,69.00),
(67,64,'LS2026106410001064',6,45,'桌面加湿器','/products/p45-1.jpg',59.00,1,59.00),
(68,65,'LS2026106510001065',6,45,'桌面加湿器','/products/p45-1.jpg',59.00,1,59.00),
(69,66,'LS2026106610001066',6,46,'防滑衣架 20 个装','/products/p46-1.jpg',25.00,1,25.00),
(70,67,'LS2026106710001067',6,46,'防滑衣架 20 个装','/products/p46-1.jpg',25.00,1,25.00),
(71,68,'LS2026106810001068',6,47,'除湿盒 6 盒装','/products/p47-1.jpg',29.00,1,29.00),
(72,69,'LS2026106910001069',6,47,'除湿盒 6 盒装','/products/p47-1.jpg',29.00,1,29.00),
(73,70,'LS2026107010001070',6,48,'静音挂钟 12 英寸','/products/p48-1.jpg',69.00,1,69.00);


-- 商品评价（订单 2 中的乳胶枕、订单 7 中的漱口杯在后台有商家回复）

INSERT INTO t_review (id,user_id,user_name,user_avatar,order_id,order_no,merchant_id,product_id,product_name,product_image,rating,content,images,anonymous,reply,reply_time,create_time) VALUES
(1,2,'张三','',1,'LS2026100110001001',1,1,'麦饭石不粘炒锅 30cm','/products/p1-1.jpg',5,'锅很沉实，不粘效果确实好，煎蛋基本不用放油。29cm 口径一家三口刚好，洗锅也方便。','/products/p1-2.jpg,/products/p1-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2,2,'张三','',2,'LS2026100210001002',4,14,'纯棉加厚浴巾 70x140','/products/p14-1.jpg',5,'浴巾厚实，吸水快，洗完澡一裹就干了。尺寸够大，成年男生也不嫌小。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3,2,'张三','',3,'LS2026100310001003',6,11,'维达抽纸 24 包整箱','/products/p11-1.jpg',4,'纸确实厚，湿水不破。就是箱子里有几包压得有点扁，不影响使用。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4,2,'张三','',4,'LS2026100410001004',5,17,'天然乳胶枕 枕芯','/products/p17-1.jpg',5,'乳胶枕回弹很好，睡了三天脖子明显舒服了，透气孔设计夏天不闷。','/products/p17-2.jpg,/products/p17-3.jpg',0,'感谢认可！枕头建议先在通风处晾置半天再使用，回弹会更佳。',DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5,3,'李四','',8,'LS2026100810001008',1,4,'北欧陶瓷餐具 16 件套','/products/p4-1.jpg',4,'餐具颜值在线，釉面很亮。碗比想象中深一些，装汤挺合适。包装也扎实。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6,3,'李四','',9,'LS2026100910001009',6,2,'316 不锈钢保温杯 500ml','/products/p2-1.jpg',5,'保温效果超出预期，早上装的开水，下午还是烫的。弹盖单手能开，开车喝水方便。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7,4,'王五','',10,'LS2026101010001010',5,18,'全棉四件套 1.8m 床','/products/p18-1.jpg',5,'四件套手感细腻，不闷，洗了一次也没掉色缩水。颜色和图片一致。','/products/p18-2.jpg,/products/p18-3.png',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8,4,'王五','',11,'LS2026101110001011',4,16,'陶瓷漱口杯 2 只装','/products/p16-1.jpg',3,'杯子哑光质感不错，但一只杯口有轻微瑕疵，联系客服补发了一个，态度还行。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9,4,'王五','',11,'LS2026101110001011',4,10,'一次性洗脸巾 80 抽','/products/p10-1.jpg',5,'洗脸巾比之前买的厚，湿水搓都不破。抽取口设计好用，一次一张不黏连。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10,5,'赵六','',12,'LS2026101210001012',4,13,'纯棉毛巾 3 条装','/products/p13-1.jpg',5,'毛巾很软，吸水也好，三条装正好分开用。就是深色那条掉了一点点浮毛，洗一次就好了。','/products/p13-2.jpg,/products/p13-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11,5,'赵六','',12,'LS2026101210001012',4,15,'浴室防滑拖鞋','/products/p15-1.jpg',4,'拖鞋轻，防滑还行，洗澡后地上有水也不打滑。建议大一码买，脚宽的话刚好。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12,5,'赵六','',15,'LS2026101510001015',1,6,'食品级硅胶铲 6 件套','/products/p6-1.jpg',5,'硅胶铲硬度刚好，炒菜不划锅，收纳桶放在台面上很整齐。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(13,5,'赵六','',18,'LS2026101810001018',1,3,'竹纤维洗碗布 5 片装','/products/p3-1.jpg',5,'第二次回购了，家里人都说好用，这次趁活动多囤了一些。','/products/p3-2.jpg,/products/p3-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(14,2,'张三','',19,'LS2026101910001019',1,3,'竹纤维洗碗布 5 片装','/products/p3-1.jpg',4,'整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(15,3,'李四','',20,'LS2026102010001020',2,5,'厨房重油污清洁剂 500ml','/products/p5-1.png',3,'收到就拆开试了，手感不错，用完再追评。客服回复也挺快。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(16,4,'王五','',21,'LS2026102110001021',2,5,'厨房重油污清洁剂 500ml','/products/p5-1.png',5,'性价比很高，比超市便宜，质量没打折扣，推荐。','/products/p5-2.jpg,/products/p5-3.jpg',0,NULL,NULL,NOW()),
(17,5,'赵六','',22,'LS2026102210001022',1,19,'多功能厨房剪刀','/products/p19-1.jpg',5,'尺寸比想象中大一些，先量好再买，做工没得挑。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(18,2,'张三','',23,'LS2026102310001023',1,20,'天然竹砧板 大号','/products/p20-1.jpg',4,'用了几天，没什么异味，清洗也方便，属于会回购的类型。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(19,3,'李四','',24,'LS2026102410001024',1,20,'天然竹砧板 大号','/products/p20-1.jpg',5,'物流稍微慢了一天，商品本身没问题，包装完整，用着顺手。','/products/p20-2.jpg,/products/p20-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(20,3,'李四','',25,'LS2026102510001025',1,21,'玻璃保鲜盒 4 件套','/products/p21-1.jpg',5,'物流稍微慢了一天，商品本身没问题，包装完整，用着顺手。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(21,4,'王五','',26,'LS2026102610001026',1,22,'双层不锈钢蒸锅 28cm','/products/p22-1.jpg',4,'买给爸妈用的，他们说很方便，操作不复杂，挺适合老人。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(22,5,'赵六','',27,'LS2026102710001027',2,7,'免手洗平板拖把套装','/products/p7-1.jpg',5,'用了两周，做工比预期好，细节处理得挺干净，包装也严实，没有磕碰。','/products/p7-2.jpg,/products/p7-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(23,2,'张三','',28,'LS2026102810001028',2,8,'洗衣机槽清洁泡腾片 12 片','/products/p8-1.jpg',4,'发货很快，第二天就到了。东西和图片一致，日常用完全够，性价比可以。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(24,3,'李四','',29,'LS2026102910001029',2,8,'洗衣机槽清洁泡腾片 12 片','/products/p8-1.jpg',5,'第二次回购了，家里人都说好用，这次趁活动多囤了一些。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(25,5,'赵六','',30,'LS2026103010001030',2,23,'洗衣凝珠 50 颗','/products/p23-1.png',3,'第二次回购了，家里人都说好用，这次趁活动多囤了一些。','/products/p23-2.jpg,/products/p23-3.png',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(26,2,'张三','',31,'LS2026103110001031',2,23,'洗衣凝珠 50 颗','/products/p23-1.png',5,'整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。',NULL,0,NULL,NULL,NOW()),
(27,2,'张三','',32,'LS2026103210001032',2,24,'加厚手提垃圾袋 100 只','/products/p24-1.jpg',5,'整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。',NULL,0,NULL,NULL,NOW()),
(28,3,'李四','',33,'LS2026103310001033',2,24,'加厚手提垃圾袋 100 只','/products/p24-1.jpg',5,'收到就拆开试了，手感不错，用完再追评。客服回复也挺快。','/products/p24-2.jpg,/products/p24-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(29,3,'李四','',34,'LS2026103410001034',2,25,'75% 酒精消毒湿巾 80 抽','/products/p25-1.jpg',5,'收到就拆开试了，手感不错，用完再追评。客服回复也挺快。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(30,4,'王五','',35,'LS2026103510001035',2,25,'75% 酒精消毒湿巾 80 抽','/products/p25-1.jpg',4,'性价比很高，比超市便宜，质量没打折扣，推荐。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(31,4,'王五','',36,'LS2026103610001036',2,26,'磁吸双面玻璃刮水器','/products/p26-1.jpg',4,'性价比很高，比超市便宜，质量没打折扣，推荐。','/products/p26-2.jpg,/products/p26-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(32,5,'赵六','',37,'LS2026103710001037',2,27,'除螨喷雾 500ml','/products/p27-1.jpg',5,'尺寸比想象中大一些，先量好再买，做工没得挑。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(33,2,'张三','',38,'LS2026103810001038',2,27,'除螨喷雾 500ml','/products/p27-1.jpg',4,'用了几天，没什么异味，清洗也方便，属于会回购的类型。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(34,3,'李四','',39,'LS2026103910001039',3,9,'无痕强力粘钩 20 枚','/products/p9-1.jpg',5,'用了几天，没什么异味，清洗也方便，属于会回购的类型。','/products/p9-2.jpg,/products/p9-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(35,4,'王五','',40,'LS2026104010001040',3,9,'无痕强力粘钩 20 枚','/products/p9-1.jpg',4,'物流稍微慢了一天，商品本身没问题，包装完整，用着顺手。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(36,2,'张三','',41,'LS2026104110001041',3,12,'真空压缩收纳袋 8 件套','/products/p12-1.jpg',5,'物流稍微慢了一天，商品本身没问题，包装完整，用着顺手。',NULL,0,NULL,NULL,NOW()),
(37,3,'李四','',42,'LS2026104210001042',3,12,'真空压缩收纳袋 8 件套','/products/p12-1.jpg',5,'买给爸妈用的，他们说很方便，操作不复杂，挺适合老人。','/products/p12-2.jpg,/products/p12-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(38,2,'张三','',43,'LS2026104310001043',3,28,'抽屉分隔收纳盒 6 件套','/products/p28-1.jpg',4,'买给爸妈用的，他们说很方便，操作不复杂，挺适合老人。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(39,3,'李四','',44,'LS2026104410001044',3,28,'抽屉分隔收纳盒 6 件套','/products/p28-1.jpg',3,'用了两周，做工比预期好，细节处理得挺干净，包装也严实，没有磕碰。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(40,3,'李四','',45,'LS2026104510001045',3,29,'免钉多层鞋架 4 层','/products/p29-1.jpg',3,'用了两周，做工比预期好，细节处理得挺干净，包装也严实，没有磕碰。','/products/p29-2.jpg,/products/p29-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(41,4,'王五','',46,'LS2026104610001046',3,30,'冰箱收纳保鲜盒 5 件套','/products/p30-1.jpg',5,'发货很快，第二天就到了。东西和图片一致，日常用完全够，性价比可以。',NULL,0,NULL,NULL,NOW()),
(42,5,'赵六','',47,'LS2026104710001047',3,31,'可折叠洗衣篮','/products/p31-1.jpg',5,'第二次回购了，家里人都说好用，这次趁活动多囤了一些。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(43,2,'张三','',48,'LS2026104810001048',3,32,'三层置物推车','/products/p32-1.jpg',4,'整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。','/products/p32-2.jpg,/products/p32-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(44,3,'李四','',49,'LS2026104910001049',3,33,'挂式真空压缩袋 5 件套','/products/p33-1.jpg',5,'收到就拆开试了，手感不错，用完再追评。客服回复也挺快。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(45,4,'王五','',50,'LS2026105010001050',4,34,'增压淋浴花洒','/products/p34-1.jpg',4,'用了两周，做工比预期好，细节处理得挺干净，包装也严实，没有磕碰。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(46,5,'赵六','',51,'LS2026105110001051',4,34,'增压淋浴花洒','/products/p34-1.jpg',3,'发货很快，第二天就到了。东西和图片一致，日常用完全够，性价比可以。','/products/p34-2.jpg,/products/p34-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(47,5,'赵六','',52,'LS2026105210001052',4,35,'浴室置物架 免打孔','/products/p35-1.jpg',3,'发货很快，第二天就到了。东西和图片一致，日常用完全够，性价比可以。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(48,2,'张三','',53,'LS2026105310001053',4,36,'硅藻泥吸水地垫','/products/p36-1.jpg',5,'第二次回购了，家里人都说好用，这次趁活动多囤了一些。',NULL,0,NULL,NULL,NOW()),
(49,3,'李四','',54,'LS2026105410001054',4,36,'硅藻泥吸水地垫','/products/p36-1.jpg',5,'整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。','/products/p36-2.jpg,/products/p36-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(50,3,'李四','',55,'LS2026105510001055',5,37,'水洗棉夏凉被','/products/p37-1.jpg',5,'性价比很高，比超市便宜，质量没打折扣，推荐。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(51,4,'王五','',56,'LS2026105610001056',5,38,'床垫保护垫','/products/p38-1.jpg',4,'尺寸比想象中大一些，先量好再买，做工没得挑。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(52,5,'赵六','',57,'LS2026105710001057',5,39,'抗菌枕巾 2 条装','/products/p39-1.jpg',5,'用了几天，没什么异味，清洗也方便，属于会回购的类型。','/products/p39-2.jpg,/products/p39-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(53,2,'张三','',58,'LS2026105810001058',5,40,'针织沙发盖毯','/products/p40-1.jpg',4,'物流稍微慢了一天，商品本身没问题，包装完整，用着顺手。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(54,3,'李四','',59,'LS2026105910001059',5,41,'遮光窗帘 2 片装','/products/p41-1.jpg',3,'买给爸妈用的，他们说很方便，操作不复杂，挺适合老人。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(55,4,'王五','',60,'LS2026106010001060',5,42,'冰丝凉席三件套','/products/p42-1.jpg',5,'用了两周，做工比预期好，细节处理得挺干净，包装也严实，没有磕碰。','/products/p42-2.jpg,/products/p42-3.jpg',0,NULL,NULL,NOW()),
(56,5,'赵六','',61,'LS2026106110001061',6,43,'全自动晴雨伞','/products/p43-1.jpg',5,'第二次回购了，家里人都说好用，这次趁活动多囤了一些。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(57,2,'张三','',62,'LS2026106210001062',6,43,'全自动晴雨伞','/products/p43-1.jpg',4,'整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(58,2,'张三','',63,'LS2026106310001063',6,44,'无火香薰 200ml','/products/p44-1.jpg',4,'整体满意，唯一小遗憾是颜色比页面看着深一点，不影响使用。','/products/p44-2.jpg,/products/p44-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(59,3,'李四','',64,'LS2026106410001064',6,45,'桌面加湿器','/products/p45-1.jpg',5,'收到就拆开试了，手感不错，用完再追评。客服回复也挺快。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(60,4,'王五','',65,'LS2026106510001065',6,45,'桌面加湿器','/products/p45-1.jpg',4,'性价比很高，比超市便宜，质量没打折扣，推荐。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(61,4,'王五','',66,'LS2026106610001066',6,46,'防滑衣架 20 个装','/products/p46-1.jpg',4,'性价比很高，比超市便宜，质量没打折扣，推荐。','/products/p46-2.jpg,/products/p46-3.jpg',0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(62,5,'赵六','',67,'LS2026106710001067',6,46,'防滑衣架 20 个装','/products/p46-1.jpg',3,'尺寸比想象中大一些，先量好再买，做工没得挑。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(63,5,'赵六','',68,'LS2026106810001068',6,47,'除湿盒 6 盒装','/products/p47-1.jpg',3,'尺寸比想象中大一些，先量好再买，做工没得挑。',NULL,0,NULL,NULL,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(64,2,'张三','',69,'LS2026106910001069',6,47,'除湿盒 6 盒装','/products/p47-1.jpg',5,'用了几天，没什么异味，清洗也方便，属于会回购的类型。','/products/p47-2.jpg,/products/p47-3.jpg',0,NULL,NULL,NOW()),
(65,2,'张三','',70,'LS2026107010001070',6,48,'静音挂钟 12 英寸','/products/p48-1.jpg',5,'用了几天，没什么异味，清洗也方便，属于会回购的类型。',NULL,0,NULL,NULL,NOW());


-- 买家咨询店铺（一个买家在一个店铺只有一条会话）

INSERT INTO t_consult (id,user_id,user_name,user_avatar,merchant_id,shop_name,shop_logo,product_id,product_name,product_image,last_content,last_sender,last_time,user_unread,merchant_unread,create_time,update_time) VALUES
(1,2,'张三','',1,'悦家厨房旗舰店','',1,'麦饭石不粘炒锅 30cm','/products/p1-1.jpg','够用，锅深 8.5cm，炒三四个人份的菜不会溢出来。','MERCHANT',DATE_SUB(NOW(), INTERVAL 46 HOUR),0,0,DATE_SUB(NOW(), INTERVAL 50 HOUR),DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(2,3,'李四','',1,'悦家厨房旗舰店','',22,'双层不锈钢蒸锅 28cm','/products/p22-1.jpg','好的，谢谢！','USER',DATE_SUB(NOW(), INTERVAL 28 HOUR),0,1,DATE_SUB(NOW(), INTERVAL 30 HOUR),DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(3,2,'张三','',2,'洁净家清专营店','',5,'厨房重油污清洁剂 500ml','/products/p5-1.png','明白了，那我先买两瓶试试。','USER',DATE_SUB(NOW(), INTERVAL 68 HOUR),0,0,DATE_SUB(NOW(), INTERVAL 70 HOUR),DATE_SUB(NOW(), INTERVAL 68 HOUR)),
(4,4,'王五','',3,'收纳研究所','',30,'冰箱收纳保鲜盒 5 件套','/products/p30-1.jpg','另外我们随盒送了 2 个冰袋，冷冻更均匀。','MERCHANT',DATE_SUB(NOW(), INTERVAL 2 HOUR),2,0,DATE_SUB(NOW(), INTERVAL 6 HOUR),DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5,5,'赵六','',4,'暖居卫浴馆','',15,'浴室防滑拖鞋','/products/p15-1.jpg','按平时运动鞋的码数拍就行，脚背高或者要穿袜子可以大一码。','MERCHANT',DATE_SUB(NOW(), INTERVAL 25 HOUR),1,0,DATE_SUB(NOW(), INTERVAL 26 HOUR),DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(6,3,'李四','',5,'眠光家纺','',40,'针织沙发盖毯','/products/p40-1.jpg','可以，装洗衣袋冷水轻柔档，平铺晾干，别拧。','MERCHANT',DATE_SUB(NOW(), INTERVAL 9 HOUR),0,0,DATE_SUB(NOW(), INTERVAL 12 HOUR),DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(7,2,'张三','',6,'日用百货铺','',NULL,NULL,NULL,'有的，在「家居日用」分类里搜「加宽衣架」就能看到，实体肩宽 20cm。','MERCHANT',DATE_SUB(NOW(), INTERVAL 99 HOUR),0,0,DATE_SUB(NOW(), INTERVAL 100 HOUR),DATE_SUB(NOW(), INTERVAL 99 HOUR)),
(8,4,'王五','',1,'悦家厨房旗舰店','',3,'竹纤维洗碗布 5 片装','/products/p3-1.jpg','好的，麻烦尽快～','USER',NOW(),0,1,DATE_SUB(NOW(), INTERVAL 1 HOUR),NOW());


-- 咨询消息明细（USER 买家 / MERCHANT 商家）

INSERT INTO t_consult_message (id,consult_id,sender_type,sender_id,sender_name,content,create_time) VALUES
(1,1,'USER',2,'张三','你好，这款麦饭石炒锅能用电磁炉吗？',DATE_SUB(NOW(), INTERVAL 50 HOUR)),
(2,1,'MERCHANT',1,'悦家厨房旗舰店','可以的，锅底是复合导磁片，电磁炉、燃气灶、电陶炉都能用。',DATE_SUB(NOW(), INTERVAL 49 HOUR)),
(3,1,'USER',2,'张三','30cm 的直径，三口之家炒菜够用吗？',DATE_SUB(NOW(), INTERVAL 47 HOUR)),
(4,1,'MERCHANT',1,'悦家厨房旗舰店','够用，锅深 8.5cm，炒三四个人份的菜不会溢出来。',DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(5,2,'USER',3,'李四','蒸锅收到啦，第一次用需要开锅吗？',DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(6,2,'MERCHANT',1,'悦家厨房旗舰店','不锈钢锅用温水加一点洗洁精洗净擦干就行，第一次蒸东西前先蒸 10 分钟清水去味。',DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(7,2,'USER',3,'李四','好的，谢谢！',DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(8,3,'USER',2,'张三','这个清洁剂对不锈钢灶台会有腐蚀吗？',DATE_SUB(NOW(), INTERVAL 70 HOUR)),
(9,3,'MERCHANT',2,'洁净家清专营店','不会，配方中性偏碱，不锈钢、瓷砖、玻璃都能用，避开铝制品就行。',DATE_SUB(NOW(), INTERVAL 69 HOUR)),
(10,3,'USER',2,'张三','明白了，那我先买两瓶试试。',DATE_SUB(NOW(), INTERVAL 68 HOUR)),
(11,4,'USER',4,'王五','保鲜盒能直接进微波炉吗？',DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(12,4,'MERCHANT',3,'收纳研究所','可以，盒身耐 -20℃~120℃，微波加热记得把盖子的四个卡扣打开。',DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(13,4,'USER',4,'王五','从冷冻室拿出来直接微波会不会裂？',DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(14,4,'MERCHANT',3,'收纳研究所','建议先解冻 5 分钟或者用中低火，骤冷骤热对任何塑料都不友好～',DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(15,4,'MERCHANT',3,'收纳研究所','另外我们随盒送了 2 个冰袋，冷冻更均匀。',DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(16,5,'USER',5,'赵六','拖鞋平时穿 42 码，拍 42 还是 43？',DATE_SUB(NOW(), INTERVAL 26 HOUR)),
(17,5,'MERCHANT',4,'暖居卫浴馆','按平时运动鞋的码数拍就行，脚背高或者要穿袜子可以大一码。',DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(18,6,'USER',3,'李四','盖毯会起球吗？家里有猫。',DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(19,6,'MERCHANT',5,'眠光家纺','针织面料做过抗起球处理，正常使用不会大面积起球；被猫爪勾到的话把浮线剪掉就好。',DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(20,6,'USER',3,'李四','能机洗吗？',DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(21,6,'MERCHANT',5,'眠光家纺','可以，装洗衣袋冷水轻柔档，平铺晾干，别拧。',DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(22,7,'USER',2,'张三','你们家有 20cm 宽的衣架吗？肩宽的衣服挂着容易变形。',DATE_SUB(NOW(), INTERVAL 100 HOUR)),
(23,7,'MERCHANT',6,'日用百货铺','有的，在「家居日用」分类里搜「加宽衣架」就能看到，实体肩宽 20cm。',DATE_SUB(NOW(), INTERVAL 99 HOUR)),
(24,8,'USER',4,'王五','洗碗布什么时候发货？',DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(25,8,'MERCHANT',1,'悦家厨房旗舰店','今天就发出，晚上物流单号会同步到订单里。',NOW()),
(26,8,'USER',4,'王五','好的，麻烦尽快～',NOW());

-- 商家提现表

CREATE TABLE `t_withdraw` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  audit_by VARCHAR(64),
  KEY idx_merchant (merchant_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 售后申请表

CREATE TABLE `t_after_sale` (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  update_time DATETIME,
  KEY idx_user (user_id),
  KEY idx_merchant (merchant_id),
  KEY idx_order_item (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------------
-- 网络安全与运维模块（访问审计 / 异常告警 / IP 黑名单）
-- ------------------------------------------------------------------

CREATE TABLE `t_access_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `uri` VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
  `method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP 方法',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端源 IP',
  `user_id` BIGINT DEFAULT NULL,
  `role` VARCHAR(20) DEFAULT NULL,
  `status_code` INT DEFAULT NULL COMMENT 'HTTP 响应状态码',
  `cost_ms` BIGINT DEFAULT NULL COMMENT '接口耗时(ms)',
  `user_agent` VARCHAR(255) DEFAULT NULL,
  `risk_flag` TINYINT DEFAULT 0 COMMENT '0正常 1限流 2黑名单 3可疑payload 4登录失败',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_access_log_time` (`create_time`),
  KEY `idx_access_log_ip` (`client_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_security_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `alert_type` VARCHAR(40) DEFAULT NULL COMMENT 'QPS_SPIKE/ERROR_RATE/SLOW_RESPONSE/IP_BURST',
  `level` VARCHAR(10) DEFAULT NULL,
  `metric_name` VARCHAR(40) DEFAULT NULL,
  `metric_value` DECIMAL(12,2) DEFAULT NULL,
  `baseline_value` DECIMAL(12,2) DEFAULT NULL,
  `score` DECIMAL(8,2) DEFAULT NULL COMMENT '异常评分',
  `detail` VARCHAR(500) DEFAULT NULL,
  `handled` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_alert_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `t_ip_blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ip` VARCHAR(64) NOT NULL,
  `reason` VARCHAR(200) DEFAULT NULL,
  `source` VARCHAR(20) DEFAULT NULL COMMENT 'AUTO自动封禁 / MANUAL人工添加',
  `expire_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_blacklist_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
