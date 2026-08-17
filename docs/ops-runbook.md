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
| **流式回复中途断掉、错误含「流累计时长超限」** | `buzhou.stream.cancelled{reason=deadline}`（spec 46 §B） | 慢滴流防护截断（默认 10m）——确属长流场景调 `buzhou.core.stream-total-timeout`（≤0 关闭）；偶发则查模型吞吐 |
| **流式取消占比异常** | `buzhou.stream.cancelled{reason}` 三路拆解 | client 升 = 前端断开行为变化；deadline 升 = 模型/网络变慢（配合 TTFT）；guard 升 = 护栏拦截频次 |
| **反馈写入被拒（rateTurn）** | 异常文案含修法 | type 三型/值域/source 两值/轮次须已存在——按文案修参；关闭会话不可反馈 |
| **金丝雀/降级候选被限流跳过** | `buzhou.resilience.rate-limit-rejected{model}` | 候选池配额闸（spec 49 §B）——提 `rate-limit.*` 或减候选流量；非模型故障，不入熔断窗 |
| **评估 run error 项突增** | `eval.run.completed` 事件 `errored` 字段 / EvalQueryService 明细 | 评估会话执行异常（模型/装配）——查明细 detail 异常摘要；偶发=provider 抖动，持续=评估环境配置错 |
| **负反馈回流全 skippedMissingReply** | FeedbackImporter 返回计数 | 负反馈轮无 assistant 回复（护栏拦截轮不可回流——预期行为）；若正常轮也跳过，查消息历史完整性 |

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
| `buzhou.core.index-closed-retention`（impl-178 键名修正：原 index.* 键与组件路径不符被静默吞） | 30d | 索引 CLOSED/DELETED 行保留期（-1 永久）；过期惰性清扫（1/64 概率 ≤256 条） |
| `buzhou.spill.encryption-key` | 关 | spill 落盘 AES-256-GCM 加密（Base64 32B；推荐环境变量注入）；开启后旧明文文件兼容读 |
| `buzhou.store.read-degrade` | off | `empty` = 消息读失败降级空历史续聊（WARN + `buzhou.stores.read-degraded` 计数可感；模型看不到历史是已知代价） |
| `buzhou.webhook.close-drain-timeout` | 5s | 停机排空已到期 webhook 记录的预算（与容器终止宽限期对齐） |
| `buzhou.tools.run-command.max-output-bytes` | 5MB | run_command 输出内存兜底上限（低内存容器收紧；截断带标记可见） |

## 4. 容量规划

- **并发会话**：`max-concurrent-sessions` ≈ 单实例可承载活跃会话数（虚拟线程执行，内存为主约束）。
- **模型吞吐**：`rate-limit.requests-per-minute / tokens-per-minute` 默认是**进程级**容量
  （多实例 = N 倍额度，见 §6）；`store.type=redis` 时自动升级为 **Redis 共享闸**（全实例
  合计一份额度，spec 54）；TPM 按真实 usage 记账，provider 不回 usage 时记 0 留痕。
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

单进程组件：熔断器 / 日配额计数 / InMemory 审计环 / SpawnGate 容量闸——**多实例 =
每实例独立额度**（限流已升级为可选共享闸，见下）。可行部署：粘性路由（会话归同实例）+
租约独占（跨实例接管走 steal）。分布式熔断/配额为显式 out-of-scope（spec 23）。

**共享限流闸（effort #14 / spec 54）**：`buzhou.store.type=redis` 且配置
`buzhou.resilience.rate-limit.requests-per-minute / tokens-per-minute` 时，限流自动从
进程内令牌桶切换为 **Redis 分钟固定窗**（INCR/EXPIRE，LiteLLM Router 同款）——全实例
共享同一份 RPM/TPM 额度（总闸正确，不再 N 倍超额）。运维须知：

- **整形差异**：固定窗在窗口边界两窗相接时可处理 2× 速率（尖峰）；额度总量与拒绝语义
  与令牌桶两档等价——按总量规划，不按瞬时速率兜底。
- **故障语义 fail-fast**：Redis 不可达时限流调用**按错误上抛**（STORE_WRITE_FAILED 带修法），
  **不静默 fail-open**——限流失效比暂不可用更危险；Redis 是该部署形态的硬依赖。
- 窗口键 = `buzhou:<模型净化名>:<RPM|TPM>:<epoch分钟窗>`（UTC epoch 时基，跨时区实例同窗），
  TTL 61s 自动滚动；无需人工清理。
- 单进程部署（无 store.type=redis）行为零变化（内存令牌桶默认）。

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
| `buzhou.model.ttft` | P95 持续劣化（如 >5s，业务定） | 流式首字变慢——排队（provider 侧）/模型负载；配合 `buzhou.model.tpot` 区分首字慢 vs 吐字慢 |
| `buzhou.model.tpot` | P95 持续劣化 | 吐字节奏变慢——模型过载典型症状（换模型/降并发） |
| `buzhou.stream.cancelled{reason=deadline}` | 窗口速率突增 | 截断风暴——慢滴流/超时预算过紧 |
| `buzhou.resilience.shadow.calls{outcome=error}` | 持续非零 | shadow 模型故障（不影响用户）；探测失真，修 shadow 端点 |
| `buzhou.resilience.shadow.calls{outcome=skipped-budget}` | 长期 100% | shadow 日预算耗尽——提 `shadow.daily-budget` 或降采样 |
| `eval.run.completed`（passRate） | 环比跌超业务阈值（如 >10pp） | 质量回归——对比 latestRun 与历史 run 明细定位坏例；回流新负反馈扩充数据集 |
| `eval.run.completed`（errored 占比） | > 20% | 评估环境故障（模型/装配）——修评估环境后重跑；errored 项不参与 passRate 分子 |


## 8. 流量治理与反馈运营（effort #10 / spec 48–49）

### 金丝雀（weighted canary）

- 配置：`buzhou.resilience.fallback.canary-enabled=true` + `weights.<modelName>=N`（未列名=1）。
  首次调用按会话稳定哈希加权抽取初始目标（同会话粘住）；`canary.selected` 事件可见首选落点。
- **变更语义**：权重/候选变更后存量会话在下一次首选解析时漂移一次（新会话即新分布）——
  灰度窗口预期行为，非故障。
- 故障回退不受权重影响：所选目标终态失败按链序试剩余候选（含原主模型），全败上抛所选目标
  原始错误。

### shadow 探测

- 配置：`buzhou.resilience.shadow.enabled=true` + `models=[bean 名]`（未命中启动失败）；
  `max-concurrent`（默认 2）/`daily-budget`（默认 1000，UTC 日池，提交次数口径）。
- 语义红线：不回注用户、不重放工具循环（裸调用）；失败吞噬（计数 outcome=error）。
- 预算池尽即停（skipped-budget 计数可感）；对照数据看 `shadow.compared` 事件
  （primaryMs/shadowMs/deltaMs/tokens）——换模型决策依据。
- 信任前提：shadow 模型与主模型同信任域（prompt 内容会发给 shadow 提供方）。

### 反馈运营（rateTurn / core.feedback）

- 接入：`session.rateTurn(turnSeq, type, value, comment, source)`（user 显式 / implicit 系统隐式）。
- 落点：state store `buzhou.feedback.*` 键 + `turn.feedback` 事件（webhook 订阅零改造）。
- 评估回流：`SessionExport.extensions["core.feedback"]` 段含 `negative` 极性标记与
  `negativeTurnSeqs` 汇总——负反馈轮可直接筛入离线评估集。
- 日志关联（MDC）：chat 轮次期间日志携带 `buzhou.sessionId`/`buzhou.turnSeq`——
  logback pattern 加 `%X{buzhou.sessionId:-}` 即与会话对齐（流式路径不支持，结构性限制
  见 spec 47 §A）。

## 9. 评估运营（effort #11 / spec 52）

### 评估数据集治理

- 存储：state store 合成会话 `__buzhou.eval__`（键 `eval.ds.<name>` 元数据 +
  `eval.ds.<name>.item.<000001>` 条目）；与业务数据同持久化面，跨重启不丢。
- 命名：`[a-z0-9-]{1,64}`（含点/斜杠/空格被拒——键结构注入防护）；建议
  `<领域>-<类型>`（如 `support-badcases`、`sales-regression`）。
- 溯源：回流项带 sourceSessionId/sourceTurnSeq——评估项可回查原始会话定位上下文。
- 删除：deleteDataset 不级联删 run 记录（run 自带 datasetName 快照，审计保留）。

### 回流策略

- 一键回流：`FeedbackImporter.importFromFeedback(sessionId, datasetName)`——只入负反馈轮
  （boolean=false / numeric 负值；categorical 无极性不入）。
- 幂等：同会话同轮重复回流跳过（skippedDuplicate）——可放心周期执行。
- 无 assistant 回复轮（护栏拦截/中断）跳过（skippedMissingReply）——不造空期望项。
- 建议节奏：日终批量回流 + 人工抽检数据集质量（防低质负反馈污染回归集）。

### run 与评估器

- run：`EvalRunner.run(datasetName, evaluator)` 顺序执行、项粒度会话隔离
  （`eval-<runId>-i<itemId>`）、单项异常不断批；passRate = passed/total（空集 0.0）。
- 评估器：内置 EXACT（trim 全等）/ CONTAINS（子串）/ REGEX（find 语义，非法正则构造期拒）；
  领域断言实现 `Evaluator` SPI 即插（LLM-as-judge 由宿主自实现，框架不内置）。
- 完成：`eval.run.completed` 事件外发（webhook 同通道）；查询面 EvalQueryService
  （runs/run/latestRun 只读）。

### 资源说明

- 评估会话走普通 spawn（占会话容量配额）——大批量 run（>百项）建议低峰执行；
  会话项粒度即开即关，不留残留。
- run 记录含 actual 预览（2048 字符截断）——明细膨胀有界；dataset 无条目数硬顶
  （治理面自律，超大集建议拆分）。

### 精确响应缓存（effort #12 / spec 53）

- 配置：`buzhou.resilience.response-cache.enabled=true` + `max-entries`（默认 256）+
  `ttl`（默认 1h）。默认关——开启前评估适用面。
- **适用场景**：固定 FAQ 前缀、评估/回归 run、幂等重试重放；同请求（model+messages+options
  采样）第二次起零模型调用、零 token 成本、不进熔断窗。
- **不适用**：强时效回复（价格/库存）、带用户身份差异输出的场景（键不含身份维度）、
  每轮 messages 都变的会话续聊（键随 messages 变化自然 miss）。
- 边界红线：带 toolCalls 的中间态响应不缓存（工具副作用安全）；空/异常响应不写；
  过期条目命中路径惰性清除（不返回陈旧）。
- 命中异常排查：`hit/miss/evicted` 计数（advisor 暴露 store）——miss 高先查 messages 视图
  是否每请求漂移（memory 注入内容含时间戳/随机数会使键失效）；evicted 高提 max-entries。

### 语义缓存（effort #15 / spec 55，LiteLLM semantic caching 同思想）

- 配置：`buzhou.resilience.semantic-cache.enabled=true`（**默认关**）+
  `similarity-threshold`（默认 0.95）+ `max-entries`（默认 128）+ `ttl`（默认 1h）；
  需注册 Spring AI `EmbeddingModel` bean（如 OpenAI starter 自动装配）——enabled 而无
  bean 启动即失败（fail-fast 带修法）。
- **机制**：精确缓存（+450）miss 后，问题文本嵌入 → 同桶（model+options）最近邻
  cosine ≥ 阈值即命中——同义问法（非逐字相同）零模型调用；带 toolCalls 不缓存
  （与精确缓存同界）；命中不进熔断窗。
- **成本口径（开启前算账）**：命中省一次模型调用、花一或两次嵌入调用（查询+写入）——
  模型贵/嵌入便宜的 FAQ 型负载最划算；会话续聊（每轮 messages 变化）收益有限。
- **残余风险（诚实声明）**：语义判别力归嵌入模型——「X」与「不是 X」若被嵌入模型编码
  为相近向量，语义缓存会错误重放（否定句场景）。框架保证阈值/分桶/边界正确；误命中
  风险靠高阈值（调低阈值 = 增召回也增误命中）+ 默认关闭 + 适用面自律（强时效/强否定
  语义敏感场景不开）承担。红队测试钉住机制语义（SemanticCacheRedteamTest）。
- 嵌入服务故障：查询/写入嵌入失败 → 该调用旁路直通主路径（bypass 计数可感，不阻断）。
- 进程内向量存储（桶内线性扫描，量级数百——perf 哨兵钉住）；跨实例共享语义缓存
  out-of-scope（RediSearch 向量另议）。

### 配置治理（effort #13 / spec 治理面）

- **绑定矩阵防线**：`ConfigBindingsMatrixTest`（starter）对全模块 metadata 键做真实装配路径
  绑定断言——新增 yml 键未注册矩阵即失败；「按文档配置静默不生效」类缺陷（T187/impl-178
  共修复 5 处）在 CI 必红。
- **公共面快照**：`docs/api-surface.snapshot.txt`（466 类型）与 `ApiSurfaceSnapshotTest`
  比对——有意变更流程：regenerateSnapshot 覆写 → 人工核对 diff → api-surface.md 同步入档。
  快照比对仅在 reactor 联编（全仓 verify）生效，单模块跑跳过（诚实边界）。
- **键拼写防护**：错拼键不报错（Spring ignoreUnknownFields）——IDE 经 metadata 提示是第一道；
  疑似键不生效时跑绑定矩阵定位（新键必须同时落 metadata + 矩阵登记）。
- **新键检查单**：metadata 入档 → 矩阵 SAMPLE_OVERRIDES/ENV_READ_KEYS 登记 → runbook §3
  调优表按需补录 → 多构造器 record 需 @ConstructorBinding（T187 教训）。
