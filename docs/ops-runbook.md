# 生产部署与运维 Runbook

> effort #5 / T97 / impl-72。配套：[spec 00 总览](spec/00-overview.md)、[spec 15 韧性](spec/15-model-resilience.md)、
> [spec 16 成本配额](spec/16-cost-quota.md)、[spec 21 质量基建](spec/21-config-supply-quality.md)。
> 定位：SRE 接手 Buzhou 生产实例的第一站文档。

## 1. 部署形态

- **单实例起步**（默认语义）：全部机制单进程内闭环。多实例的限制见 §6（粘性路由 + 独占租约
  可部署，分布式配额/限流 out-of-scope）。
- **存储选型**：`buzhou.store.type = memory`（默认，进程内）→ `jdbc`（MySQL/PostgreSQL/H2，
  引 `buzhou-store-jdbc` + DataSource）→ `redis`（引 `buzhou-store-redis`）。拼写错误启动即失败
  （fail-fast，带可选值指引）。
- **依赖**：JDK 21+、Spring Boot 4.1、Spring AI 2.0.0；POSIX shell（run_command 场景）。
- **发布构件**：`buzhou-spring-boot-starter`（引入即得全部机制自装配）+ `buzhou-bom` 版本收口。

## 2. 故障排查树（症状 → 定位 → 处置）

| 症状 | 定位 | 处置 |
|------|------|------|
| 全部模型调用快速失败，异常含「熔断器拒绝」 | `buzhou.resilience.circuit.*`；health details `circuitStates`；`buzhou.resilience.circuit-tripped` 指标 | provider 故障——查上游；熔断 30s 后自动半开探测；配 fallback.models 降级保命 |
| 模型持续 5xx/超时重试耗尽 | `retry-exhausted` 事件 / `buzhou.resilience.retry-exhausted` | 调 `max-attempts`/`deadline`；确认 `retryable-categories`；升级 provider |
| 会话第 N 轮后回复「已达到会话 token/成本预算上限」 | `budget.token-hard-stop` / `budget.cost-hard-stop` 事件 | 有意拦截——业务决定：提额（`buzhou.token-budget.*`）或新开会话（fork 可继承历史、预算重置） |
| 回复「已达到本会话当日配额上限」 | `quota.exceeded` 事件（dimension） | UTC 自然日自动重置；紧急提额改 `buzhou.resilience.session-quota.*` |
| 回复「已达到…步数/时长上限」 | `runaway.hard-stop` 事件 | 死循环保护——查 `runaway.soft-threshold` 前的模型行为；误伤则调 `buzhou.runaway.*` |
| run_command 回复「命令沙箱不可用」 | 沙箱探测失败（deno 缺失等） | 按 unavailableHint 装依赖；或 `buzhou.tools.command.backend=builtin` 回退内置档（接受较弱隔离） |
| 日志刷「MCP server 工具集漂移」 | `mcp.tools-drift` 事件 | server 端工具变更——触发配置 refresh 或重启会话吸收；回调绑定在装配期，热替换不支持 |
| 事件外发 webhook 全败日志 | `buzhou.webhook.failures` 指标 | 查端点/签名密钥一致性（4xx 不重试=配置错）；队列溢出看 `buzhou.webhook.dropped` |
| **下游反馈 webhook 事件缺失** | `buzhou.webhook.dead-letter` 指标 + `WebhookOutboxHealth` details（pending/deadLetters） | 端点修复后 `replayDeadLetters()` 一键重放（attempts 清零重投；4xx 死信先查消费端契约）；频繁超限看 outbox-capacity |
| **会话列表/过滤查询缺失或降级** | dashboard `IndexedSessionPage.fromIndex=false` | SessionIndexStore 未装配——jdbc/redis store.type 自动配；内存部署需显式定义 bean（spec 30 降级语义） |
| 会话跨实例接管冲突 | `SESSION_ALREADY_ACTIVE` / 租约日志 | 用 steal 语义接管；确认部署是粘性路由（见 §6） |
| **chat 报「会话已有在途轮次（单飞闸）」** | `TURN_IN_FLIGHT`（NON_RETRYABLE） | 误用暴露而非故障：同会话轮次不并发——等在途终结或改新会话；前端防双击/网关防重放即可根除 |
| **spill 读报「解密失败（密钥不匹配或文件损坏）」** | 换钥重启读旧密文文件（spec 40 §A） | 用写入时的 encryption-key 读（密钥历史保留）；确认没把不同环境密钥混用 |
| **启动失败「检测到未来 schema 版本」** | SchemaMigrator 守卫（spec 42 §A） | 旧构建对上新库被拒——升构建到库版本；不要绕过（防旧 schema 写坏新库） |
| **启动失败「已应用迁移脚本被改动」** | 版本表 checksum 锚（spec 42 §A） | 已应用脚本不可变——恢复脚本原文；新变更用新版本号脚本 |
| 启动失败「store.type=jdbc 但对应 store 实现未装配」 | store 类型守卫 | 引 buzhou-store-jdbc + 确认 DataSource bean |

## 3. 配置调优表（高频项）

| 键 | 默认 | 调优场景 |
|----|------|---------|
| `buzhou.resilience.circuit.failure-rate-threshold` | 0.5 | provider 抖动多 → 0.7 防误跳；持续硬故障 → 0.3 快速止血 |
| `buzhou.resilience.circuit.open-cooldown` | 30s | 探测频率；半开打爆担忧 → 调大 |
| `buzhou.resilience.fallback.models` | 无 | 生产保命链：主模型挂 → 备模型接管（熔断 OPEN 恒触发） |
| `buzhou.resilience.session-quota.tokens-per-day` | 不限 | 多租户滥用防护；按用户分级配 |
| `buzhou.token-budget.max-session-total-tokens` | 不限 | 单会话成本上限；配 `pricing.*` 后可 `max-session-cost-usd` |
| `buzhou.token-budget.pricing.<model>.input/output-per-million` | 无价 | 成本计量前提（USD/百万 token）；无价 = 只计 token |
| `buzhou.runaway.per-turn.max-steps` | 不限 | 工具循环防护；生产建议显式设置（如 24） |
| `buzhou.backpressure.max-concurrent-sessions` | 不限 | 实例容量闸；排队超时 `spawn-queue-timeout` |
| `buzhou.skills.catalog-cache-ttl` | 30s | DB Skill 热更新延迟容忍度；0=直查 |
| `buzhou.webhook.url/secret` | 关 | 事件外发；secret 配置后带 HMAC-SHA256 签名头 |
| `buzhou.mcp.shutdown-budget` | 35s | 停机排空预算 |
| `buzhou.resilience.circuit.backoff-cap` | 8 | 长故障探测频率指数放缓上限（×open-cooldown） |
| `buzhou.resilience.circuit.half-open-success-threshold` | 1 | 抖动 provider：2-3 提高恢复置信度（连续成功才 CLOSE） |
| `buzhou.webhook.outbox-capacity` | 10000 | 下游长期不可用时的未决积压上限（满则拒入+计数） |
| `buzhou.tools.result-limit-chars` | 20000 | 工具结果入上下文护栏；频繁截断（`result-truncated` 指标）→ 优化该工具或 per-tool 覆盖 |
| `buzhou.skills.catalog-max-entries` | 64 | 目录注入体积护栏（截断附「另有 N 个未列出」提示） |
| `buzhou.index.closed-retention` | 30d | 索引 CLOSED/DELETED 行保留期（-1 永久）；过期惰性清扫（1/64 概率 ≤256 条） |
| `buzhou.spill.encryption-key` | 关 | spill 落盘 AES-256-GCM 加密（Base64 32B；推荐环境变量注入）；开启后旧明文文件兼容读 |
| `buzhou.store.read-degrade` | off | `empty` = 消息读失败降级空历史续聊（WARN + `buzhou.stores.read-degraded` 计数可感；模型看不到历史是已知代价） |
| `buzhou.webhook.close-drain-timeout` | 5s | 停机排空已到期 webhook 记录的预算（与容器终止宽限期对齐） |
| `buzhou.tools.run-command.max-output-bytes` | 5MB | run_command 输出内存兜底上限（低内存容器收紧；截断带标记可见） |

## 4. 容量规划

- **并发会话**：`max-concurrent-sessions` ≈ 单实例可承载活跃会话数（虚拟线程执行，内存为主约束）。
- **模型吞吐**：`rate-limit.requests-per-minute / tokens-per-minute` 是**进程级**容量（多实例 =
  N 倍额度，见 §6）；TPM 按真实 usage 记账，provider 不回 usage 时记 0 留痕。
- **成本**：会话累计进 SessionStateStore（跨崩溃持久）；日配额按 UTC 日窗重置。
- 性能基线参考（Apple Silicon 单机）：每轮 harness 开销 P95 ≈ 0.6ms、微压缩 ≈ 1.8M msgs/s
  （docs/perf/baseline.md——跨机不可比，只看同机趋势）。

## 5. 升级与回滚

- **升级**：BOM 收口同版本演进；JDBC store schema 迁移走既有 migration 链（impl-31）——升级前
  备份 + 演练；`buzhou-bom` 升级后全模块同版本（enforcer 禁止漂移）。
- **回滚**：回滚二进制即可（前向兼容的 store schema 例外——见 migration 说明）；会话历史/摘要/
  状态在 store 中跨版本持久，回滚不丢。
- **灰粒度**：机制级开关（`buzhou.<mod>.enabled`）可独立关闭回退底座行为（如 resilience=false
  回退 Spring AI 原生重试语义）。

**存储一致性对账（spec 29）**：怀疑存储泄漏/孤儿数据时，跑 `StoreFsck.run(stores)`
只读报告（四检测项：孤儿摘要/残留 state/泄漏租约/悬挂观测；全集 = 观测留痕 + extras
补充）——先看报告再决定是否 `repair`（按检测项可选，观测永不自动清）。

**跨 store 迁移（effort #8）**：store 切换（JDBC↔Redis/缩容下线）逐会话
`SessionMigrator.migrate(source, target, sessionId, keepIds)`（默认新 Id 重映射；
keepIds 需目标空闲）；演练见 examples Effort8CapabilitiesDemoTest。

**会话级备份恢复（effort #6/#7）**：关键会话定期 `exportSession(id).toJson()` 落档
（messages+summary+state+扩展段，如 memory.facts）；恢复 = `importSession(json, false)`
新 Id 重映射后以该 Id spawn 续用（灾难恢复演示见 examples Effort6CapabilitiesDemoTest）。
JDBC 部署升级至 V3（会话索引表）由 SchemaMigrator 版本化自动执行（旧库基线判定后追加，
无破坏性变更）。迁移器防护（effort #9 / spec 42 §A）：**未来版本拒绝**（旧构建对新高版本库启动
即拒——升级顺序先升构建）与**脚本 checksum 锚定**（已应用脚本事后被改启动即拒；存量行首次升级
自动回填锚定）。

**spill 加密密钥管理（effort #9 / spec 40 §A）**：`buzhou.spill.encryption-key`（Base64 32B，
推荐 `BUZHOU_SPILL_ENCRYPTIONKEY` 类环境变量注入）开启即密文落盘；轮换 = 换新值重启（**旧密文
文件需旧钥读**——保留密钥历史或接受旧 spill 过期清扫）；磁盘全明文时代文件升级后兼容可读。
DB/Redis at-rest 属部署层盘加密职责（TLS + 磁盘加密），不归本键。

**审计签名密钥轮换（effort #9 / spec 41 §A）**：`buzhou.guard.audit.signing.key-dir` 指目录即启用
约定命名（`v<version>.pem` PKCS#8 + `v<version>.pub.pem` 可选）；运行期 `rotate(N, keyPair)` 写而后切
（落盘失败轮换中止）；重启按目录扫描自动入环（active=最高版本）。**链外锚点**：定期取
`AuditChainVerifier.verify(...).headHash()` 存入保险库/异地，校验时传锚点比对——删尾/整链重写可检
（纯内部校验盲区）。

## 6. 多实例边界（诚实声明）

单进程组件：限流桶 / 熔断器 / 日配额计数 / InMemory 审计环 / SpawnGate 容量闸——**多实例 =
每实例独立额度**。可行部署：粘性路由（会话归同实例）+ 租约独占（跨实例接管走 steal）。
分布式限流/配额/熔断为显式 out-of-scope（spec 23）。

**webhook outbox（spec 24）**：outbox 落共享 state store（JDBC/Redis）时事件跨重启不丢，
但多实例分发器可能**双投递**——at-least-once 契约内，消费端以 `X-Buzhou-Event-Id` 幂等
去重是契约责任；内存 store 部署等价旧进程内暂存（重启丢在途）。

**健康端点新维度（effort #8）**：`webhook-outbox`（pending/deadLetters/delivered/dropped
水位——恒 UP，告警走指标面）与 `session-index`（wired/hasRows 采样探测——未装配时该面
不注册，属预期降级非故障）。

## 7. 告警项清单（指标 → 阈值 → 动作）

| 指标 | 建议阈值 | 动作 |
|------|---------|------|
| `buzhou.resilience.circuit-tripped` | 任意一次（窗口内重复>2 告警） | 查 provider 健康；确认 fallback 生效 |
| `buzhou.resilience.fallback-exhausted` | > 0（P1） | 主备全挂——立即人工介入 |
| `buzhou.resilience.retry-exhausted` | 窗口速率突增 | provider 抖动前兆 |
| `buzhou.budget.hard-stops` | 业务定义 | 成本失控或攻击性使用 |
| `buzhou.backpressure.spawn-rejected` | 持续非零 | 容量不足——扩容或提闸 |
| `buzhou.webhook.failures` | 持续非零 | 外发端点故障（消费方告警） |
| `buzhou.webhook.dead-letter` | 任意（P2） | 事件进死信（重试耗尽/4xx）——`deadLetters()` 定位 eventId，按需重放 |
| `buzhou.webhook.dropped` | 任意 | outbox 容量满拒入——下游不可用时长超退避窗口，扩 outbox-capacity 或修下游 |
| `buzhou.tools.result-truncated` | 窗口速率持续非零 | 工具结果频繁被截断——定位高频工具优化其返回（分页/聚合），或 per-tool 覆盖 |
| `buzhou.observability.queue.wait` | P95 持续 > 500ms | 观测管线满队阻塞背压（at-least-once 不丢的代价）——store 写入慢：查存储健康/批量参数（spec 39 §B） |
| `buzhou.mcp.tools-drift` | 任意 | 工具面变更未同步——refresh |
| `buzhou.mcp.connect.failures` | 持续非零 | MCP server 不可达 |
| guard 审计链断链 WARN | 任意 | 审计链完整性——立即人工（合规风险） |
