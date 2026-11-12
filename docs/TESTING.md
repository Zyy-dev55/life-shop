# 回归测试与断言体系

本仓库把测试按系统分层组织，**失败信息能直接指出故障归属层**，而不是只给一句「接口挂了」。
三层测试互相补充：

| 层 | 位置 | 技术 | 运行方式 | 覆盖对象 |
| --- | --- | --- | --- | --- |
| 后端单元测试 L2/L4/L6 | `backend/src/test/java/com/lifeshop/unit/` | JUnit 5 + Mockito + AssertJ | `cd backend && mvn test` | 拆单/防超卖、售后状态机、资金闭环、JWT、安全拦截器、EWMA 异常检测 |
| 后端集成测试 L1~L3 | `backend/src/test/java/com/lifeshop/integration/` | `@SpringBootTest` + MockMvc + H2 | 同上 | 真实容器下的协议、拦截器链顺序、鉴权、店铺数据边界 |
| 前端单元测试 L1/L4/L6 | `frontend/tests/*.spec.js` | Vitest | `cd frontend && npm test` | 展示工具、响应判定、前后端状态机一致性、API 路径契约 |
| 端到端分层回归 L0~L6 | `tests/regression.mjs` | Node 内置 `node:test` + `assert` | `node tests/regression.mjs` | 真实 HTTP 链路、跨层审计落库、SPA 资源 |
| 分层排障探测 | `tests/diagnose.mjs` | Node 内置 | `node tests/diagnose.mjs` | 逐层体检，输出第一个失败层与排查建议 |

> `tests/regression.mjs` 与 `tests/diagnose.mjs` **零第三方依赖**，Node 18+ 直接可跑，
> 适合在无网络、U 盘拷贝、网吧等环境里验证。

---

## 一、快速运行

```bash
# 1) 后端 41 个用例（含 Spring 容器集成测试，H2 内存库，不需要 MySQL）
cd backend
mvn test

# 2) 前端 20 个用例
cd frontend
npm ci
npm test

# 3) 端到端分层回归（需先启动服务）
cd backend
java -jar target/life-shop-server.jar --spring.profiles.active=demo
# 另开终端
node tests/regression.mjs http://127.0.0.1:8080

# 4) 分层排障体检（可选 --probe 复现登录防爆破链路）
node tests/diagnose.mjs http://127.0.0.1:8080
```

演示模式数据存在 H2 内存库，**重启即还原**，所以端到端用例全部设计成只读，
可以反复运行而不污染基线。`regression.mjs` 每轮使用一个独立的 `X-Forwarded-For`
源 IP，避免上一轮的限流窗口影响下一轮。

---

## 二、分层与断言清单

失败时按下表反查：断言内容 → 对应层 → 最可能的原因。

### L0 运行环境

| 断言 | 失败含义 |
| --- | --- |
| TCP 端口可达 | 后端进程没起、端口被占用、profile 不对 |
| Node >= 18 / fetch 可用 | 运行脚本的 Node 版本过旧 |

### L1 网络 / 协议层

| 断言 | 失败含义 |
| --- | --- |
| `/api/auth/health` → HTTP 200 且 `code=200` | DispatcherServlet 未启动、Controller 未被扫描、启动日志有异常 |
| `Content-Type: application/json` | 请求被静态资源回退或网关改写 |
| 安全响应头四项齐全 | `SecurityInterceptor` 未注册到 `/api/**` |
| `POST /api/auth/health` → 4xx | HTTP 方法映射被误改 |
| OPTIONS 预检 < 400 | CORS 配置或拦截器放行分支失效 |
| 前端 API 契约：路径/方法一致 | 前端改了 URL 但后端 `@RequestMapping` 没跟上 |

### L2 安全拦截层

| 断言 | 失败含义 |
| --- | --- |
| XSS / SQLi / UNION / 路径穿越 → 403 | 特征正则被削弱，或安全拦截器被移出链外层 |
| 403 响应体不含 `Exception` / 包名 | 错误体泄露内部实现细节 |
| `X-Forwarded-For` 首段作为源 IP | 反向代理下会把代理 IP 当成客户端，限流与封禁全部错位 |
| 黑名单命中 → 403 + `RISK_BLACKLIST` 留痕 | `blockCache` 与数据库未同步 |
| 登录连续失败 → 429 并自动封禁 | `login-limit` / `autoBlock` 链路断开 |
| 正常中文关键词 → 200 | 正则过宽导致误拦截 |
| afterCompletion 审计字段齐全 | 日志采集缺失会让运维看板与异常检测失去数据源 |

### L3 身份鉴权层

| 断言 | 失败含义 |
| --- | --- |
| 无令牌 / 伪造令牌 → `code=401` | `JwtInterceptor` 未注册，或 `JwtUtil.parse` 抛异常而非返回 null |
| JWT 往返还原 userId/username/role | 密钥或 claim 名称改动 |
| 篡改 / 过期 / 异密钥令牌 → `parse()` 返回 null | 签名校验被绕过（高危） |
| 买家访问 `/api/admin/**` → 403 | `AdminInterceptor` / `UserContext.requireAdmin` 失效 |
| 商家订单列表只含本店 `merchantId` | 店铺数据边界被破坏（多租户串数据） |

### L4 业务服务层

| 断言 | 失败含义 |
| --- | --- |
| 多店铺一次结算 → 两笔订单、运费各自计算 | 拆单逻辑退化成一单，运费与结算全错 |
| 满 99 免运费 | 阈值配置或比较方向错误 |
| 库存扣减 SQL 含 `stock = stock - n` 且带 `stock >= n` 条件 | 退化成「先查后写」，并发下会超卖 |
| 条件更新影响 0 行 → 抛异常且不落订单 | 事务边界被破坏，会生成无库存订单 |
| 下架商品 / 他人地址 → 拒绝下单 | 前置校验缺失 |
| 售后申请金额被截断为商品实付 | 可超额退款 |
| `apply()` 返回 `typeText` / `statusText` 非空 | 历史缺陷：漏调 `fillText`，前端标签空白 |
| 仅退款 → 3；退货退款 → 1 → 2 → 3；拒绝 → 4 | 状态机被改乱 |
| 重复处理 / 错误状态 / 越权 → 异常 | 并发或越权下可重复退款 |
| 退款 SQL 含 `balance = GREATEST(balance - x, 0)` | 只改状态不扣余额，资金账目不闭环 |
| 前后端状态机镜像一致 | 两端流转规则漂移 |

### L5 数据持久层

| 断言 | 失败含义 |
| --- | --- |
| 全量商品 `stock >= 0` 且 `sales >= 0` | 超卖或库存回滚错误 |
| 钱包四个金额字段非负 | 退款/提现重复扣减 |
| 审计表出现 L2 产生的 403 记录 | 异步写线程未工作、`t_access_log` 缺失 |
| 突发流量 → 评分 ≥ 3.0 且四类告警命中 | EWMA 基线 / 权重 / 量纲下限被改动 |
| 平稳流量 → 评分 < 3.0、无误报 | 阈值或方差下限失当 |
| 有效窗口 < 5 → `WARMUP` 且不告警 | 冷启动误报 |

### L6 前端呈现层

| 断言 | 失败含义 |
| --- | --- |
| 首页含 `<div id="app">` | 前端产物未打进 jar |
| `/after-sale` 回退到 index.html | history 路由刷新白屏 |
| 首页引用的 `/assets/*` 全部 200 | chunk 缺失、缓存的旧 index.html |
| `money()` 对 NaN/null 输出 `0.00` | 页面显示 NaN |
| `classifyResponse` 只解包 `code=200` | 把错误响应当成功数据渲染 |
| 401 独立分类 | 登录失效时不清理令牌、不跳转 |

---

## 三、跨层回归点（最值得保留的三条）

1. **L2 → L5**：`regression.mjs` 先在 L2 打 4 个攻击载荷，再在 L5 断言
   `status-distribution` 里出现 ≥4 条 403、`overview.suspiciousHits >= 4`。
   这条用例同时验证「拦截生效」「审计入队」「异步落库」「统计聚合」四段链路。
2. **L4 → L5**：后端单测断言退款 SQL 落在商家余额上，端到端断言钱包金额非负，
   两者共同锁住资金闭环。
3. **后端 → 前端**：`AfterSaleService` 的状态常量与 `frontend/src/utils/afterSale.js`
   的镜像函数在两组测试里被分别断言，任一侧改动流转规则都会被发现。

---

## 四、如何新增一条回归用例

1. 先确定它属于哪一层，放进对应的 `describe` / 测试类，命名以 `should...` 开头；
2. 写**显式断言**，不要用 `console.log` 或「不抛异常就算过」；
3. 断言失败信息里带上实际值，例如 `assert.equal(r.status, 403, 'XSS 未被拦截')`；
4. 端到端用例保持只读；确需写库时用 H2 演示模式并在文档注明「需重启还原」；
5. 在本文「断言清单」补一行「断言 → 失败含义」，让下一个人能直接定位。

---

## 五、CI

`.github/workflows/ci.yml` 在 push / PR 时依次执行：

1. `mvn test`（JDK 17）；
2. `npm ci && npm test`；
3. `npm run build` 验证前端可构建；
4. 启动 demo 模式的 jar，运行 `node tests/regression.mjs`；
5. 上传 Surefire 报告作为构建产物。

端到端阶段用 `curl` 轮询 `/api/auth/health` 等待服务就绪，避免启动竞态。
