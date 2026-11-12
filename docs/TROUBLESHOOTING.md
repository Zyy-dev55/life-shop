# 分层排障手册

排障的基本原则：**自底向上，先确定故障层，再在该层内部定位**。
上层现象往往只是下层故障的投影——「页面白屏」可能是 L6 的 chunk 404，
也可能是 L0 的服务根本没起来；先跑一次 `node tests/diagnose.mjs`，
它会输出第一个失败层，避免一上来就翻业务代码。

```
L0 运行环境 → L1 网络/协议 → L2 安全拦截 → L3 身份鉴权
      → L4 业务服务 → L5 数据持久 → L6 前端呈现
```

诊断命令（任选其一，零依赖）：

```bash
node tests/diagnose.mjs                       # 逐层体检
node tests/diagnose.mjs --probe              # 额外复现登录防爆破 / 自动封禁
node tests/regression.mjs                    # 分层回归断言，失败即退出码 1
```

---

## L0 运行环境

**范围**：JDK / Node / 端口 / profile / 构建产物是否齐全。

| 现象 | 检查点 | 命令 |
| --- | --- | --- |
| 浏览器完全连不上 | 8080 是否监听、jar 是否存在 | `netstat -ano \| findstr :8080`（Windows）/ `lsof -i :8080` |
| 启动即退出 | 是否带 demo profile、JDK 版本 | `java -jar target/life-shop-server.jar --spring.profiles.active=demo` |
| 连上了但全是 404 | 前端产物没打进 jar | `jar tf target/life-shop-server.jar \| grep static/index.html` |
| MySQL 连接报错 | 用了默认 profile 而非 demo | 演示模式用 H2，不需要 MySQL |

常见根因：忘记 `--spring.profiles.active=demo`；端口被上一次的进程占用；
jar 是改前端之前打的旧包。

---

## L1 网络协议层

**范围**：TCP 连通、HTTP 状态码、Content-Type、CORS、安全响应头。

| 现象 | 检查点 | 命令 |
| --- | --- | --- |
| 502 / 连接被重置 | 反向代理是否指向 8080 | `curl -i http://127.0.0.1:8080/api/auth/health` |
| 返回 HTML 而不是 JSON | 请求被 SPA 回退接管 | 看 `Content-Type`；`/api/**` 不应回退到 index.html |
| 跨域预检失败 | `addCorsMappings` 与 OPTIONS 放行 | `curl -i -X OPTIONS -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET" http://127.0.0.1:8080/api/user/info` |
| 安全响应头缺失 | 安全拦截器是否注册 | 响应头应有 `X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、`Content-Security-Policy-Report-Only` |
| 错误 HTTP 方法返回「200 + 业务码 500」 | 通用异常兜底吞掉了 `HttpRequestMethodNotSupportedException` | `curl -i -X POST http://127.0.0.1:8080/api/auth/health`；正确应返回 HTTP 405 |

> **回归测试实际拦下的一次退化**：早期 `GlobalExceptionHandler` 只有 `@ExceptionHandler(Exception.class)`，
> 把方法不匹配的异常兜底成 500，客户端与网关会把协议错误误判成服务端故障，运维看板的错误率也被污染。
> 现已补充 `HttpRequestMethodNotSupportedException` 专用处理，返回 HTTP 405 + `code=405`，
> 并由 `ApiRegressionTest.shouldReturn405ForUnsupportedMethod` 与 `regression.mjs` 的 L1 断言共同守住。

> 部署在 Nginx 之后时，源 IP 依赖 `X-Forwarded-For` 首段透传。
> 若代理没配 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;`，
> 所有请求都会被归到代理 IP，限流和自动封禁会误伤全站用户。

---

## L2 安全拦截层

**范围**：IP 黑名单、滑动窗口限流、登录防爆破、攻击载荷过滤、访问审计。

| 现象 | 检查点 | 命令 |
| --- | --- | --- |
| 正常请求被 403 | 查询串里是否含 `or 1=1`、`../`、`<script` 等特征 | `curl -i "http://127.0.0.1:8080/api/product/page?q=<script>"` |
| 压测时大面积 429 | 全局阈值 120 次/分钟、登录 8 次/分钟 | 看运维看板 `policy`，或临时调 `lifeshop.security.global-limit` |
| 攻击流量没被拦 | 拦截器链顺序 | `WebMvcConfig.addInterceptors`：`SecurityInterceptor` 必须最外层 |
| 看板没有拦截统计 | 审计日志入队/落库 | 查 `t_access_log`，`risk_flag=3` 为载荷命中 |

**故障注入示例（可复现的排障演练）**：

```bash
# 1) 触发登录防爆破：连续 9 次失败登录，第 9 次应返回 429 并自动封禁 10 分钟
for i in $(seq 1 9); do
  curl -s -o /dev/null -w "%{http_code} " -X POST \
    -H "Content-Type: application/json" -H "X-Forwarded-For: 198.51.100.77" \
    -d '{"username":"nobody","password":"wrong"}' \
    http://127.0.0.1:8080/api/auth/login
done
# 期望输出：401 401 ... 401 429

# 2) 被封禁 IP 的后续请求应直接 403
curl -i -H "X-Forwarded-For: 198.51.100.77" http://127.0.0.1:8080/api/auth/health

# 3) 在管理员看板 /admin/sec-ops 解除封禁，或等待 10 分钟自动失效
```

`node tests/diagnose.mjs --probe` 会自动完成这三步，并换一个独立演练 IP，
不会封掉本机 `127.0.0.1`。

> **测试环境陷阱（本项目真实踩过）**：MockMvc 的 `.param()` 只填参数表，
> 不生成 `getQueryString()`；而安全拦截器检查的是原始 query string。
> 集成测试里必须用 `.queryParam()` 才会触发拦截，否则会出现
> 「本地测试 200、真实请求 403」的假象。见 `ApiRegressionTest.shouldBlockXssBeforeController`。

---

## L3 身份鉴权层

**范围**：JWT 签发/解析、登录拦截器、角色拦截器、店铺归属。

| 现象 | 检查点 | 命令 |
| --- | --- | --- |
| 明明带 token 仍 401 | 头格式必须是 `Authorization: Bearer <token>`；令牌是否过期（7 天） | `curl -i -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/user/info` |
| 401 后前端没跳登录 | `classifyResponse` 是否把 401 单独分类 | 见 `frontend/tests/response.spec.js` |
| 商家看到别店订单 | `merchantService.currentId()` 是否作为过滤条件 | `GET /api/merchant/order/page` 断言 `merchantId` 一致 |
| 管理员接口 403 | 角色是否 ADMIN、拦截器是否注册 | `UserContext.requireAdmin()` |

拦截器链顺序必须是：
`SecurityInterceptor → JwtInterceptor → Admin/MerchantInterceptor → Controller`。
把安全层放到鉴权层之后，未登录的攻击流量就不会被审计留痕。

---

## L4 业务服务层

**范围**：拆单、运费、库存、售后状态机、余额变动。

| 现象 | 检查点 | 定位命令 / 断言 |
| --- | --- | --- |
| 一次结算只生成一单 | 是否按 `merchantId` 分组 | `OrderSplitAndStockTest.shouldSplitByMerchantAndChargeFreightPerOrder` |
| 运费算错 / 满 99 未免运 | `freight` 与 `freeFreightThreshold` 配置 | 单测断言 `payAmount = 金额 + 运费 - 抵扣` |
| 并发下超卖 | 扣库存是否为带条件的原子 UPDATE | 断言 SQL 含 `stock = stock - n` 与 `stock >= n` |
| 售后按钮不出现 / 标签空白 | `fillText` 是否在返回前调用 | `AfterSaleStateMachineTest.shouldCapRefundAmountAndFillDisplayText` |
| 售后状态卡住 | 状态机 0→1→2→3 / 0→3 / →4 / →5 | `frontend/tests/after-sale.spec.js` 与后端状态机镜像互校 |
| 退款后余额没变 | `doRefund` 是否执行 | 断言 SQL 含 `balance = GREATEST(balance - x, 0)` |

**故障复现示例：资金账目不闭环**

```bash
# 现象：售后单显示「退款完成」，但商家钱包余额没有减少
# 分层定位：L4 状态流转正常 → L5 余额更新缺失
# 1) 查售后单状态与金额
curl -s -H "Authorization: Bearer $BUYER_TOKEN" \
  "http://127.0.0.1:8080/api/after-sale/my?pageNum=1&pageSize=10" | jq '.data.list[0] | {status,statusText,amount}'
# 2) 查商家余额
curl -s -H "Authorization: Bearer $MERCHANT_TOKEN" \
  http://127.0.0.1:8080/api/merchant/wallet | jq '.data.balance'
# 3) 根因：handleByMerchant 只 update 状态、漏调 doRefund；
#    修复后由 AfterSaleStateMachineTest 的两条余额 SQL 断言守住。
```

---

## L5 数据持久层

**范围**：表结构、约束、审计日志、告警记录、资金与库存的最终一致性。

演示模式可在 `http://127.0.0.1:8080/h2-console` 直接查库
（JDBC URL 见 `application-demo.yml`，用户 `sa`，空密码）。

```sql
-- 库存是否被扣成负数
SELECT id, name, stock, sales FROM t_product WHERE stock < 0 OR sales < 0;

-- 资金侧：余额是否出现负值
SELECT id, name, balance FROM t_merchant WHERE balance < 0;

-- 审计链路：L2 的攻击是否落库
SELECT status_code, risk_flag, COUNT(*) FROM t_access_log
GROUP BY status_code, risk_flag ORDER BY status_code;

-- 异常检测：告警是否正常生成
SELECT alert_type, level, score, handled FROM t_security_alert ORDER BY id DESC LIMIT 20;
```

| 现象 | 常见根因 |
| --- | --- |
| `t_access_log` 为空 | `SecOpsService.init()` 的写线程没起来，或建表脚本缺少该表 |
| 看板统计为 0 但业务正常 | 异步队列写入异常被吞掉（审计失败不应影响业务，需查日志） |
| 告警全是误报 | EWMA 预热窗口不足 5、量纲下限 `SCALE_FLOOR` 被删、权重改错 |
| 突发流量不告警 | `ALERT_THRESHOLD` 被调高，或日志时间没有落在最近 12 个分钟窗口 |

> 动态基线检测可用 `POST /api/admin/secops/simulate` 做流量回放：
> 注入约 220 条正常日志 + 160 条突发日志，正常情况评分约 7.5（阈值 3.0），
> 命中 QPS_SPIKE / ERROR_RATE / SLOW_RESPONSE / IP_BURST 四类告警。

---

## L6 前端呈现层

**范围**：静态资源、SPA history 回退、展示工具、错误提示。

| 现象 | 检查点 | 命令 |
| --- | --- | --- |
| 刷新子页面白屏 | `PathResourceResolver` 是否回退 index.html | `curl -i http://127.0.0.1:8080/after-sale` |
| chunk 404 | 前端产物是否重新构建并打进 jar | `npm run build:server` 后重新 `mvn package -DskipTests` |
| 页面金额显示 NaN | `money()` 对 null/NaN 的兜底 | `frontend/tests/format.spec.js` |
| 接口报错但页面没提示 | `classifyResponse` 分支 | `frontend/tests/response.spec.js` |
| 只改了前端却没生效 | 浏览器缓存了旧 index.html | 强制刷新；jar 内的 static 是否被更新 |

**故障复现示例：白屏**

```bash
# 1) 看首页引用的资源
curl -s http://127.0.0.1:8080/ | grep -o '/assets/[^"]*' | head
# 2) 逐个请求，找出 404
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/assets/<上面某个文件名>
# 3) L6 判定：资源 404 属于前端产物问题，不是接口问题；
#    重新构建前端并重打 jar，不要去改后端 Controller。
```

---

## 诊断输出怎么读

`diagnose.mjs` 每层打印 `✓ / ! / ✗`，末尾给出汇总与
「建议从 Lx 开始排查」。判读规则：

- **下层全绿、上层红**：优先查两层之间的配置（拦截器注册、路由、序列化、构建产物）。
- **同层多项同时红**：该层组件整体未生效（例如安全拦截器没注册，会导致响应头、403、审计同时失败）。
- **`!` WARN**：多为缺少前置条件（如管理员登录失败导致 L5 跳过），先修上层再重跑。
- **只有 `--probe` 后 L2 变红**：是演练 IP 被自动封禁的预期结果，等 10 分钟或在看板解封。

回归断言（`regression.mjs`）与排障探测（`diagnose.mjs`）的分工：
前者给出「哪条断言不满足」，后者给出「应该从哪一层开始查」。
完整的断言清单见 [TESTING.md](./TESTING.md)。
