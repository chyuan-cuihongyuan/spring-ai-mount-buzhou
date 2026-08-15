# Spec 50 — 错误码统一与退避卫生（effort #10）

> effort #10 第五篇。§A：错误码统一收口（T178）；§B：退避 jitter 补全（T179）。
> 语义借鉴：Spring Boot（~78K★）错误面分类纪律；本仓 ErrorCode（14 码 + RetryCategory）
> 既有单一事实源——本篇是「跨模块外部可见面渐进挂码」，不是新机制。

## §A 错误码统一收口（T178 / impl-147）

### Problem Statement

core 之外的模块（guard 41 / spill 18 / skills 13 / tools 10 / store 16+5 处泛化 throw）大量
裸 `IllegalStateException`/`UncheckedIOException`——运维侧无法按 ErrorCode/RetryCategory 自动
分诊，告警与策略面断裂在模块边界。

### Solution

外部可见的运行失败面渐进挂 `BuzhouException`+ErrorCode（新码按需增配 RetryCategory）；
编程式 API 误用类 `IllegalArgumentException`（keyVersion 校验等内部断言）**保留**——
它们是调用方 bug 的断言信号，不是运行故障。

### Implementation Decisions

- 新码：`SPILL_IO_FAILED`(RETRYABLE)、`STORE_READ_FAILED`(RETRYABLE)、
  `SKILL_OPERATION_INVALID`(NON_RETRYABLE)。
- 迁移面（渐进首批）：DiskSpillStore 全部 IO 失败路径（store/读/删/清点 9 处）→
  SPILL_IO_FAILED（保留 cause）；SkillAdminApi 状态冲突 → SKILL_OPERATION_INVALID、
  依赖未装配 → CONFIG_INVALID；TodoStore 序列化失败 → DATA_CORRUPTION；
  ArgumentFingerprint/ReadIntegrity「SHA-256 不可用」→ CONFIG_INVALID（JVM 环境根因）。
- 保留面（钉住不改）：JCS/SigningKeyRing/InMemory 容量等 `IllegalArgumentException`
  （编程式断言）；spill「one call one spill」契约 ISE（调用方违约断言）。
- 破坏性变更入档（api-surface T186）：上述迁移点异常类型变化（ISE/UncheckedIOException →
  BuzhouException 子类语义，message 保留）。

### Testing Decisions

- skills：重复上架/下架未上架 → BuzhouException + SKILL_OPERATION_INVALID（既有测试升级断言）。
- spill：IO 失败（目标根路径被文件占据）→ BuzhouException + SPILL_IO_FAILED + cause 保留。
- 回归：四模块全量 verify 绿（含既有断言升级）。

### Out of Scope

- 剩余 ~70 处内部断言与低频 throw 的全量迁移（后续 effort 按模块渐进）。
- 错误码进日志 MDC（日志结构化属观测面扩展）。

## §B 退避 jitter 补全（T179 / impl-148）

### Problem Statement

模型重试链已有 jitter（±0.5 默认），但 webhook outbox（`min(1s×2^attempts,60s)`）与
policy 轮询失败退避是纯指数——多实例同相位重试形成雷鸣羊群（thundering herd）。

### Solution

两处退避公式加 ±25% 随机抖动（模型重试链 jitter 同语义收窄）；确定性测试注入 Random。

### Implementation Decisions

- WebhookEventForwarder 退避：`base ± 25%`（`ThreadLocalRandom`，下限不破 1s 量级语义——
  抖动后仍 ≥ 0.75×base）；测试经可注入 `LongSupplier` 随机源钉住确定性。
- DbPolicyConfigProvider 轮询失败退避同口径。
- 事件/指标零变化（抖动是纯时间行为）。

### Testing Decisions

- 注入随机源（0.0/0.5/1.0 三点）断言退避区间边界；回归全量绿。

### Out of Scope

- 全局限流重试的 jitter（限流器无重试回路）。
