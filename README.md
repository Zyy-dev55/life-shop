# 悦家生活 · 生活用品电商系统

一套可直接运行的多店铺生活用品电商系统，包含**商城前台**、**个人中心**、**商家后台**、**平台管理后台**四块。

> **项目描述**：系统面向买家、商家、管理员三类角色，覆盖商品与分类、购物车、订单、支付、评价咨询、优惠券、物流追踪、售后退款、商家钱包提现与平台监管等功能；以店铺标识作为数据边界、按店铺拆单并分开计算运费、用条件更新防止超卖、把退款与提现统一落在商家余额上保证资金闭环。网络侧实现源 IP 识别、黑名单与滑动窗口限流、登录防爆破、XSS/SQL 注入/路径穿越过滤、安全响应头与访问审计，并基于 EWMA 动态基线与加权异常评分实现智能运维告警。完整描述与可直接粘贴到 GitHub About 的文案见 [docs/PROJECT_OVERVIEW.md](./docs/PROJECT_OVERVIEW.md)。
  
覆盖「浏览 → 咨询客服 → 加入购物车 → 下单（按店铺拆单）→ 支付 → 商家发货 → 收货 → 评价 → 商家回复」完整交易闭环。

技术栈：Java 8 + Spring Boot 2.7 + MyBatis-Plus + MySQL 8 / H2 + JWT + Vue 3 + Vite + Pinia + Element Plus。

内置演示数据：**6 家店铺 / 6 大类 / 48 件商品 / 65 条评价 / 70 笔订单 / 8 组客服会话**，全部配真实商品图，开箱即有内容可点。

系统是**多店铺**模型：商品归属店铺，买家一次结算混了多家店铺的商品时，会按店铺拆成多笔订单，
  
商家只能看到、也只能操作自己店铺的商品与订单。

---

## 一、最快跑起来（不需要装 MySQL）

后端内置了 **H2 内存库演示模式**，启动时自动建表并灌入演示数据，关掉就还原，随便折腾。

### Windows 一键启动

```bat
start-demo.bat      :: 启动后端（8080），首次会等 10 秒左右
start-frontend.bat  :: 启动前端（5173），浏览器打开 http://localhost:5173
```

> 前端产物已经打进 jar，**只跑 `start-demo.bat` 就够了**，直接访问 <http://localhost:8080> 就是完整系统。
> `start-frontend.bat` 只在需要改前端、要热更新时用（5173 会把 `/api` 代理到 8080）。

### 命令行启动

```bash
# 后端（已编译好的 jar，直接跑）
java -jar backend/target/life-shop-server.jar --spring.profiles.active=demo --server.port=8080

# 或者用 Maven 跑源码
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=demo

# 前端
cd frontend && npm install && npm run dev
```

打开 <http://localhost:5173> 即可。

> 演示模式数据存在内存里，重启后端就回到初始状态（这一点在调 bug 时很方便）。

---

## 二、正式运行（MySQL 8）

```bash
# 1. 建库建表 + 灌数据（脚本会自动 DROP 同名库，注意不要执行到已有库上）
mysql -uroot -p < sql/life_shop.sql

# 2. 改数据库账号密码
#    backend/src/main/resources/application.yml → spring.datasource.username / password

# 3. 启动
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

想重新灌一遍数据：再执行一次 `sql/life_shop.sql` 即可。

---

## 三、账号

| 账号                            | 密码         | 角色    | 登录后进入                            |
| ----------------------------- | ---------- | ----- | -------------------------------- |
| `admin`                       | `admin123` | 平台管理员 | 平台管理后台 `/admin`                  |
| `zhangsan`                    | `123456`   | 买家    | 商城前台（有购物车、地址、多笔订单、待评价商品、3 个咨询会话） |
| `lisi` / `wangwu` / `zhaoliu` | `123456`   | 买家    | 商城前台（有历史订单与已发表评价）                |

商家账号（登录后自动进商家中心 `/merchant`，每家店只经营一个分类）：

| 账号             | 密码       | 店铺      | 经营类目 |
| -------------- | -------- | ------- | ---- |
| `shop_kitchen` | `123456` | 悦家厨房旗舰店 | 厨房烹饪 |
| `shop_clean`   | `123456` | 洁净家清专营店 | 清洁除菌 |
| `shop_storage` | `123456` | 收纳研究所   | 收纳整理 |
| `shop_bath`    | `123456` | 暖居卫浴馆   | 卫浴洗漱 |
| `shop_bedding` | `123456` | 眠光家纺    | 家纺床品 |
| `shop_daily`   | `123456` | 日用百货铺   | 家居日用 |

三个后台入口：前台右上角会根据角色显示「管理后台」或「商家中心」；直接访问 `/admin`、`/merchant` 也能进，
  
但路由守卫和后端拦截器都会校验角色，跨角色访问一律 403。

---

## 四、功能清单

### 商城前台

- 首页：轮播（暖色渐变 + 商品实拍合成图）、分类导航、热销榜、新品
- 商品列表：关键词搜索、分类筛选、销量/价格排序、分页
- 商品详情：多图切换、到手价与划线价、库存、评分与评价数、**评价区**（评分概览 / 星级分布 / 只看有图 / 晒图预览 / 商家回复）
- **咨询商家**：商品详情页「咨询商家」按钮唤起聊天抽屉，带当前商品上下文、快捷提问、5 秒轮询等商家回复；「购物保障」卡片也有「联系店铺客服」入口
- **我的咨询**：左侧会话列表（按最近消息排序、未读角标），右侧聊天记录，顶部导航与个人中心都有入口
- 购物车：勾选结算、数量加减、库存校验、删除与清空、满 99 包邮
- 结算：地址选择与新增、运费规则、模拟支付
- 我的订单：状态筛选与角标、支付 / 取消 / 确认收货、进度条、**逐件「去评价」**
- 个人中心：资料修改、改密码、**待评价列表**、**我的评价**（含商家回复）
- 收货地址：增删改、设为默认

### 商家后台 `/merchant`（商家账号登录后进入）

- 经营概览：今日销售额 / 今日订单 / 待发货 / 待回复评价四张卡，7 日销售 + 订单双轴图（ECharts），热销商品 Top5，库存预警统计，头部提示「N 条客户咨询等你回复」
- 商品管理：只列出本店商品，新增（支持直接上传主图）/ 编辑 / 上下架 / 删除
- 订单管理：只列出本店订单，单号/收货人/手机号检索、状态筛选、详情抽屉、**发货**
- 评价管理：本店商品评价，按「待回复 / 已回复」筛选、回复与修改回复
- **客户咨询**：本店会话列表（未读高亮、只看未读、按昵称/内容检索）、聊天记录、商品上下文、快捷回复模板、一键已读；侧边栏带未读数角标，30 秒自动刷新
- 店铺设置：店铺名称、Logo（可上传）、简介、联系人、电话、发货地址

数据边界全部在服务端：`/api/merchant/**` 的每条查询都带 `merchant_id = 当前店铺`，
  
修改类接口会先校验归属，拿到别家的商品 ID / 订单 ID / 会话 ID 也改不动（一律 403）。

### 平台管理后台 `/admin`（管理员登录后进入）

- 数据看板：GMV、订单数、用户数、近 7 天销售趋势（ECharts）、状态分布
- 商品管理：全平台商品，新增 / 编辑 / 上下架 / 删除，列表带「所属店铺」列
- 分类管理：增删改，分类下有商品时禁止删除
- 订单管理：全平台订单，单号/收货人/手机号检索、状态筛选、订单详情抽屉、一键发货
- 评价管理：评价检索与星级筛选、**回复 / 修改回复**、删除（自动重算商品评分）
- 用户管理：检索、启用 / 禁用

---

## 五、交易与数据设计要点

- **防超卖**：下单时用条件更新 `UPDATE t_product SET stock = stock - n WHERE id = ? AND stock >= n`，
    
  影响行数为 0 直接抛「库存不足」，配合 `@Transactional` 回滚整单。
- **快照**：订单保存商品名/图/单价，收货地址保存完整字符串，后续商品改价不影响历史订单。
- **取消回补**：按订单明细逐条把库存加回、销量减回（`GREATEST(sales - n, 0)` 防止负数）。
- **评价与评分**：评价按 `(order_id, product_id)` 唯一，只能对「已完成」订单的明细评价；
    
  提交/删除评价后自动重算 `t_product.rating` 与 `review_count`；匿名评价在展示层脱敏昵称与头像。
- **按店铺拆单**：下单时先按商品的 `merchant_id` 分组，每组生成一笔独立订单（各自算运费），
    
  订单保存店铺名称快照，明细也带 `merchant_id`；一次结算买 3 家店 = 3 笔订单、3 次模拟支付。
- **数据隔离**：商家端接口以 `merchant_id` 作为唯一数据边界，`ProductService` / `OrderService` / `ReviewService`
    
  都提供 `merchantXxx` 方法，先归属校验再操作，越权统一返回「不属于当前店铺」。
- **咨询会话**：`t_consult` 一条会话 = 一个买家在一个店铺（`UNIQUE(user_id, merchant_id)`），
    
  再次咨询复用同一条会话、只更新「最近咨询的商品」；消息按条落 `t_consult_message`，
    
  `sender_type` 区分 `USER` / `MERCHANT`。会话上冗余 `last_content / last_time / user_unread / merchant_unread`，
    
  列表页一次查询就能出排序、摘要和红点，不用扫消息表。
    
  未读数规则简单且自洽：谁发的消息就把**对方**的未读 +1，自己这一侧清零；打开会话时把自己这一侧清零。
- **三级角色**：JWT 只放 `role`，拦截器分三层——登录（`/api/**`）、管理员（`/api/admin/**`，`ADMIN`）、
    
  商家（`/api/merchant/**`，`MERCHANT`）；管理员进不了商家后台，商家也进不了平台后台。

---

## 六、商品图片说明

`frontend/public/products/` 下 144 张商品图（**48 个商品 × 3 张**）与 `public/banners/` 3 张横幅，
  
由 `scripts/fetch-images.js` 抓取公开搜索结果、`scripts/gen-banners.js` 合成生成，**仅用于本演示项目**。
  
换成自己的素材时，直接把文件替换掉即可（数据库里存的是 `/products/p1-1.jpg` 这类相对路径）。

商品与分类的定义集中在 `scripts/products.js`（`scripts/gen-seed.js` 生成 SQL、`scripts/fetch-images.js` 抓图都读这一份），
  
想加商品只需在数组里追加一条，再依次执行：

```bash
node scripts/fetch-images.js   # 按 keyword 抓 3 张商品图
node scripts/gen-seed.js       # 重新生成 MySQL / H2 种子数据
```

重新抓图：
  
node scripts/gen-banners.js    # 首页横幅（渐变 + 商品图合成 SVG）
  
node scripts/fetch-banners.js  # 也可以从图库抓实景图（含打水印风险，默认不用）

```

---

## 七、目录结构

```

life-shop/
  
├── backend/                        后端（Spring Boot 2.7 / Java 8）
  
│   └── src/main/
  
│       ├── java/com/lifeshop/
  
│       │   ├── common/             统一响应、分页、异常、登录上下文
  
│       │   ├── config/             JWT / 管理员拦截器、MyBatis-Plus、静态资源映射
  
│       │   ├── controller/         前台接口 + admin/ 平台后台 + merchant/ 商家后台
  
│       │   ├── dto/ vo/            入参与出参
  
│       │   ├── entity/ mapper/     实体与 Mapper
  
│       │   └── service/            业务逻辑（商品/购物车/订单/评价/咨询/统计…）
  
│       └── resources/
  
│           ├── application.yml          MySQL 正式配置
  
│           ├── application-demo.yml     H2 演示配置
  
│           └── db/                      H2 建表与演示数据
  
├── frontend/                       前端（Vue 3 + Vite）
  
│   ├── public/products/            商品图（本地）
  
│   ├── public/banners/             首页横幅（SVG）
  
│   └── src/
  
│       ├── api/                    接口封装
  
│       ├── components/             头部、底部、商品卡、评价弹窗、咨询聊天窗
  
│       ├── layout/                 前台 / 平台后台 / 商家后台布局
  
│       ├── store/                  用户、购物车
  
│       └── views/                  页面（admin/ 平台后台，merchant/ 商家后台）
  
├── docs/                           项目文档（概述 / 测试 / 排障 / 发布指引）
│   ├── PROJECT_OVERVIEW.md         项目全景与可直接粘贴的简介文案
│   ├── TESTING.md                  分层回归测试与断言清单
│   ├── TROUBLESHOOTING.md          分层排障手册
│   └── PUBLISH-TO-GITHUB.md        仓库发布流程与报错对照
├── sql/life_shop.sql               MySQL 初始化脚本（建库 + 建表 + 演示数据）
  
├── scripts/                        图片处理、种子数据生成、接口自检、打包
│   ├── gen-seed.js                 生成 sql/life_shop.sql 与 H2 数据（唯一数据源）
│   ├── smoke-test.js               73 项接口自检
│   ├── check-sfc-bindings.js       静态检查模板里未定义的变量（白屏元凶）
│   ├── gen-design-figures.js       生成架构图 / E-R 图等插图（SVG -> PNG）
│   ├── fetch-images.js             抓取并压缩商品图（34MB -> 14MB）
│   ├── products.js                 商品种子数据（名称、价格、图片映射）
│   └── make-package.js             组装离线演示包
├── build/                          jlink 出的精简 JRE、原图备份（不进版本库）
└── dist/life-shop-demo/            可直接发给别人的演示包

````

---

## 八、接口一览（共 81 个）

| 模块 | 方法与路径 | 说明 |
| --- | --- | --- |
| 认证 | `POST /api/auth/login` `POST /api/auth/register` `GET /api/auth/health` | 登录 / 注册 / 健康检查 |
| 用户 | `GET/PUT /api/user/info` `PUT /api/user/password` | 资料与密码 |
| 商品 | `GET /api/product/page` `/hot` `/new` `/{id}` | 列表、热销、新品、详情 |
| 分类/轮播 | `GET /api/category/list` `GET /api/banner/list` | 公开 |
| 购物车 | `GET /api/cart/list` `/count`，`POST /add`，`PUT /quantity` `/checked` `/check-all`，`DELETE /{id}` `/clear` | 全量购物车操作 |
| 地址 | `GET /api/address/list` `/default`，`POST/PUT /api/address`，`DELETE /{id}`，`PUT /{id}/default` | 收货地址 |
| 订单 | `POST /api/order/submit`，`GET /page` `/counts` `/{id}`，`PUT /{id}/pay` `/cancel` `/confirm` | 下单（返回按店铺拆分后的订单数组）、支付、取消、收货 |
| 评价 | `GET /api/review/product/{productId}`（公开）、`GET /pending` `/my`、`POST /api/review` | 商品评价、待评价、我的评价、提交 |
| **咨询-买家** | `GET /api/consult/list` `/unread` `/with?merchantId=&productId=` `/{id}`，`POST /send` | 我的会话、未读数、打开会话、历史消息、发消息 |
| 上传 | `POST /api/upload` | 评价晒图，返回 `/uploads/xxx.jpg` |
| 后台-统计 | `GET /api/admin/statistics/dashboard` | 看板数据 |
| 后台-商品 | `GET /page` `/{id}`，`POST`，`PUT`，`PUT /{id}/status`，`DELETE /{id}` | 商品 CRUD |
| 后台-分类 | `GET /api/admin/category/list`，`POST`，`PUT`，`DELETE /{id}` | 分类 CRUD |
| 后台-订单 | `GET /api/admin/order/page` `/{id}`，`PUT /{id}/ship` | 检索与发货 |
| 后台-评价 | `GET /api/admin/review/page`，`POST /{id}/reply`，`DELETE /{id}` | 回复与删除 |
| 后台-用户 | `GET /api/admin/user/page`，`PUT /{id}/status` | 检索与禁用 |
| 商家-店铺 | `GET/PUT /api/merchant/profile`，`GET /api/merchant/dashboard` | 店铺资料、经营概览 |
| 商家-商品 | `GET /api/merchant/product/page` `/{id}`，`POST`，`PUT`，`PUT /{id}/status`，`DELETE /{id}` | 本店商品 CRUD |
| 商家-订单 | `GET /api/merchant/order/page` `/pending-count` `/{id}`，`PUT /{id}/ship` | 本店订单与发货 |
| 商家-评价 | `GET /api/merchant/review/page`，`POST /{id}/reply` | 本店评价与回复 |
| **商家-咨询** | `GET /api/merchant/consult/page` `/unread` `/{id}`，`POST /{id}/reply` `/read-all` | 本店会话、未读数、聊天记录、回复、全部已读 |

响应统一为 `{ code, message, data }`，`code=200` 成功，`401` 未登录，`403` 无权限，`500` 业务异常。

---

## 九、接口自检脚本

不想点页面，也可以一条命令把整条链路跑一遍
（平台数据 → 登录 → 加购 → 下单拆单 → 支付 → 发货 → 收货 → 评价 → 后台回复 → **商家后台数据隔离与越权拦截** → **买家咨询 / 商家回复 / 跨店越权拦截** → 权限校验）：

```bash
node scripts/smoke-test.js                 # 默认打 http://127.0.0.1:8080
node scripts/smoke-test.js http://ip:port  # 指定服务地址
````

当前通过项：**73 / 73**。

改完前端页面还可以跑一遍静态检查，专门抓「模板里写了、`<script setup>` 里没定义」的变量
  
——这类问题 `vite build` 不报错，但运行时会渲染失败，表现就是整页白屏、点不动：

```bash
node scripts/check-sfc-bindings.js   # 全部页面模板与脚本变量一致 ✓
```

---

## 十、打包发给别人（离线演示包）

`dist/生活用品商城-演示版.zip`（约 70MB）解压即用：对方**不用装 Java、不用装数据库、不用开前端**，
双击 `启动商城.bat` 就能看。前端产物已经打进 jar，由后端一个端口同时提供页面和接口。

```
life-shop-demo/
  启动商城.bat             Windows 双击启动
  start-mac-linux.command  macOS / Linux 启动
  使用说明.txt             给非技术同学看的说明书
  life-shop.jar            后端 + 前端产物（43MB）
  runtime/                 精简 JRE，Windows 专用（44MB）
  uploads/                 运行期上传的图片
```

### 自己重新打包

```bash
# 1. 前端构建进后端静态目录（classpath:/static）
cd frontend && npm run build:server

# 2. 打可执行 jar，自动带上上一步的前端产物
cd ../backend && mvn -o -DskipTests package

# 3. 生成精简 JRE，只需做一次，产物在 build/jre
jlink --module-path "$JAVA_HOME/jmods" \
      --add-modules java.base,java.desktop,java.instrument,java.logging,java.management,java.naming,java.security.jgss,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.zipfs \
      --output build/jre --strip-debug --no-header-files --no-man-pages --compress=2

# 4. 组装 dist 目录 + 压缩成 zip
cd .. && node scripts/make-package.js
```

`scripts/make-package.js` 负责复制 jar 与 runtime、写入两个启动脚本和 `使用说明.txt`、最后直接调用 Node 内置 zlib 压成 zip（不依赖系统 zip）。
压缩用 Python 的 `zipfile` 而不是 PowerShell 的 `Compress-Archive`，因为后者会把条目写成反斜杠路径、
中文文件名不带 UTF-8 标记（别人在 Mac 上解压会乱码），也没法保留可执行位。现在 zip 里的路径都是正斜杠、
文件名带 UTF-8 标记、`start-mac-linux.command` 与 `runtime/bin/*` 记录为 755。

### 几个实现细节

- **单端口托管**：`WebMvcConfig` 里注册了 `/**` 的静态资源处理，找不到文件且路径不带点号时回退 `index.html`，
  所以直接访问 `/product/1`、`/merchant/consults` 这类深链接不会 404；`/api`、`/uploads`、`/h2-console`、`/error`
  明确排除在回退之外，接口该 404 还是 404。demo profile 里同时关掉了 Boot 默认静态映射（`spring.web.resources.add-mappings=false`），
  避免两套规则打架。
- **自动选端口**：启动脚本用 `netstat` / `lsof` 从 8080 往后探测到 8090，被占用就换下一个，浏览器打开的是实际端口。
- **图片瘦身**：`scripts/fetch-images.js` 把商品图最长边压到 900px、JPEG 质量 82（渐进式、去元数据），
  34MB → 14MB，原图备份在 `build/image-backup/`。改完图片要重新 `npm run build:server` 才会进包。
- **数据在内存**：对方重启就恢复初始演示数据，随便试不会弄乱；`uploads/` 只放运行期上传的图片。
- **macOS / Linux**：包里的 runtime 是 Windows 版，脚本会自动改用系统 Java 17
  （`brew install openjdk@17` / `sudo apt install openjdk-17-jdk`），所以这两个系统仍需装 JDK。
- **zip 太大不好发**：可以只发 `life-shop-demo` 文件夹（不打 zip），或删掉 `runtime/` 让对方自己装 Java 17，
  体积能降到 43MB。

---

## 十一、系统设计文档

项目开发时另有一份《系统设计与数据库设计说明书》（28 页，含 16 张数据字典表、13 张实体属性图与全局 E-R 图）。**该文档未包含在本仓库中**，下表保留其章节结构，方便对照代码阅读：


| 章节 | 内容 |
|---|---|
| 3.1 总体结构设计 | 设计原则、技术选型表、**功能逻辑图**、分层架构图、角色权限表 |
| 3.2.1 概念设计 | 13 个实体的**实体属性图**（矩形实体 + 椭圆属性）+ **全局 E-R 图** |
| 3.2.2 逻辑设计 | 关系模式、实体联系汇总表、数据表清单 |
| 3.2.3 物理设计 | 16 张表的完整数据字典（字段 / 类型 / 允许空 / 键 / 说明 / 索引） |
| 3.2.4 关键设计说明 | 主键策略、索引、快照冗余、状态约束、数据隔离、历史归档 |
| 附录 A / B | 5 张新增表的建表语句、状态码与枚举值对照表 |

改完表结构后重新出插图：

```bash
node scripts/gen-design-figures.js
```

插图由脚本按表结构生成，加表只需追加字段定义即可重画。

---

## 十二、关于毕业论文

本项目同时是毕业设计课题。按学校要求，配套的《设计说明书》与论文正文单独提交，**未包含在本公开仓库中**（出于查重与版权考虑，相关文件已在打包时排除）。

本仓库保留的是可以直接运行、可以直接读的工程本体：后端 81 个接口、前端四个端、分层回归测试与排障手册，上面各节都有具体说明。

---

## 十三、常见问题

**1. `mvn` 或 `java` 找不到**
  
装 JDK 8+（推荐 17）与 Maven 3.6+，并配置 `JAVA_HOME`。演示模式下用 H2，不需要 MySQL。

**2. 端口被占用**
  
后端 `--server.port=8081`，前端改 `frontend/vite.config.js` 里的 `server.port`，
  
同时把代理 `target` 改成后端实际端口。

**3. 前端页面能开但接口报「网络异常」**
  
后端没起或者端口不一致。先浏览器访问 <http://localhost:8080/api/auth/health>，
  
应返回 `{"code":200,...,"data":"ok"}`。

**4. 商品图 404**
  
确认 `frontend/public/products/` 目录存在且未被清空；数据库里图片是 `/products/p1-1.jpg` 相对路径，
  
由前端静态目录提供。

**5. 支付是真的吗**
  
不是。点「立即支付」直接把订单置为「待发货」，用于演示状态流转。
  
接真实支付只需替换 `OrderService#pay`（下单后调起支付、回调里再改状态）。

**6. 评价按钮不出现**
  
只有「已完成」订单的明细才能评价，且每件商品只能评一次。确认收货后按钮就会出现。

**7. 商家后台为什么看不到全部商品 / 订单**
  
这是设计如此。系统是多店铺模型，商家端所有接口都以 `merchant_id` 为数据边界，
  
`shop_kitchen` 只能看到「悦家厨房旗舰店」的 8 件商品和本店订单。要看全平台数据，用 `admin` 进平台管理后台。

**8. 一次结算为什么生成了好几笔订单**
  
购物车里混了多家店铺的商品时会按店铺拆单（和淘宝「共 N 个包裹」一致），每家店各自算运费、各自发货。
  
结算页会按店铺分组展示商品和运费，提交后的提示里也会说明拆了几笔。

**9. 管理员能进商家后台吗 / 商家能进平台后台吗**
  
都不能。后端 `AdminInterceptor` 只认 `ADMIN`，`MerchantInterceptor` 只认 `MERCHANT`，互不兼容，
  
前端路由守卫也会拦下来并给出提示。

**10. 某个页面白屏、按钮点不动**
  
多半是模板里引用了 `<script setup>` 中不存在的变量：Vue 渲染时抛 `TypeError`，
  
整个组件树挂掉，页面就剩一片白。浏览器控制台会有 `Cannot read properties of undefined`。
  
跑 `node scripts/check-sfc-bindings.js` 能直接把文件名和变量名指出来。
  
如果刚改过代码但页面没变，先 `Ctrl + F5` 硬刷新，排除 HMR 缓存。

---

## 回归测试与断言

仓库把测试按系统分层组织，失败信息能直接指向故障层：

| 层 | 位置 | 命令 |
| --- | --- | --- |
| 后端单元 L2/L4/L6 | `backend/src/test/java/com/lifeshop/unit/` | `cd backend && mvn test` |
| 后端集成 L1~L3 | `backend/src/test/java/com/lifeshop/integration/` | 同上（MockMvc + H2，不需要 MySQL） |
| 前端单元 / 契约 | `frontend/tests/*.spec.js` | `cd frontend && npm ci && npm test` |
| 端到端分层回归 L0~L6 | `tests/regression.mjs` | 启动 demo 服务后 `node tests/regression.mjs` |
| 分层排障探测 | `tests/diagnose.mjs` | `node tests/diagnose.mjs`（可选 `--probe`） |

当前基线：**后端 41 个 JUnit 用例 + 前端 20 个 Vitest 用例 + 端到端 31 条断言**。
`regression.mjs` 与 `diagnose.mjs` 只用 Node 内置模块，无需安装依赖，且端到端用例全部只读、可反复运行。

- 断言清单、跨层回归点、新增用例规范 → [docs/TESTING.md](./docs/TESTING.md)
- L0~L6 分层排障手册、故障复现示例、常见根因对照 → [docs/TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md)

`.github/workflows/ci.yml` 在 push / PR 时自动跑后端测试、前端测试与构建、端到端分层回归。

---

## 发布到 GitHub

本仓库已排除论文正文、演示包、构建产物与依赖目录（见 `.gitignore`），可以直接公开。

- 项目描述与仓库 About 文案 → [docs/PROJECT_OVERVIEW.md](./docs/PROJECT_OVERVIEW.md)
- 网吧 / 换机发布的完整操作流程（注册、PAT、建空仓、推送、报错对照） → [docs/PUBLISH-TO-GITHUB.md](./docs/PUBLISH-TO-GITHUB.md)

命令行最短路径（在仓库根目录）：

```bash
git remote add origin https://github.com/<你的用户名>/life-shop.git
git branch -M main
git push -u origin main   # 密码栏填 Personal Access Token，不是账号密码
```
