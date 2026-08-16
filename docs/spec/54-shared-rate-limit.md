# Spec 54 — 多实例共享限流（effort #14）

> effort #14 唯一篇。§A：RateLimitBackend SPI + 内存后端平移 + ModelRateLimiter 策略/存储
> 分离（T222）；§B：Redis 固定窗后端（T223）+ starter 装配（T224）；§C：containers 共享额度
> 验证（T225）；§D：红队面（T226）；§E：perf 哨兵 + 演示 + 文档面（T227–T229）。
> 外部事实源：LiteLLM（~26K★）Router per-deployment rpm/tpm 限流 = Redis 固定窗计数
> （INCR/EXPIRE，跨实例聚合额度）。本地裁定：**不用分布式令牌桶**（Lua 状态机复杂度
> 不值）——分钟固定窗，窗口边界 2× 尖峰可能诚实入档；**不做 L1 内存 + L2 Redis 分层**
> （DualCache 一致性成本待证据，Redis 往返即共享语义本体）。

## §A RateLimitBackend SPI 与内存平移（T222 / impl-185）

### Problem Statement

ModelRateLimiter 原为单进程内存令牌桶（synchronized Bucket + ConcurrentHashMap）——
多实例部署下 RPM/TPM 额度 = N 倍（impl-74/T99 以 WARN 诚实声明）。策略（排队/拒绝/
事件）与额度存取耦合在同一类，无法替换共享存储。

### Solution

- `RateLimitBackend`（buzhou-core `core.spi`，resilience 与 store-redis 共用）：
  `tryAcquire(model, dimension, amount)`（amount ≤ 0 纯预检）/ `consume`（事后记账，
  可致负余额——诚实表达超限）/ `available`（≥0 封顶容量）/ `capacity` / 
  `secondsUntilAvailable`（等待提示）/ `kind`（memory/redis，观测面）。
  维度 = 约定字符串 "RPM"/"TPM"（常量在 resilience 侧）。
- `InMemoryRateLimitBackend`（buzhou-resilience）：原 TokenBucket 逻辑**平移**
  （capacity、refillRate = capacity/60、synchronized 桶、桶键 model:dimension）——
  默认后端，单进程行为零变化（全量既有测试绿即证明）。
- `ModelRateLimiter` 改造：保留策略层（QUEUE 有界排队 / FAIL_FAST 立即拒、事件发射、
  TPM 预检不扣减 + 调用后记账、usage 缺失记 0 留痕）；额度存取全部委托 backend。
  新增五参构造（backend 注入）与 `backend()` 观测出口；旧构造默认内存后端。

### Implementation Decisions

- SPI 放 `buzhou-core.spi`（与 Transport/UnitOfWork 同域）：store-redis 实现 resilience
  接口会引入反向依赖，core 是两模块唯一公共上游。
- 内存后端 refill 语义与原实现逐行等价（nanoTime 时基、Math.min(capacity, ...) 封顶）；
  `secondsUntilAvailable` 不可知时返回 MAX_VALUE 由策略层 queueTimeout 兜底。
- 故障语义写入 SPI 契约：实现必须线程安全；Redis 实现故障 = fail-fast 上抛，
  **不静默 fail-open**（限流失效比暂不可用更危险）。

### Testing Decisions

- resilience 全量既有测试（117 项）不改一行全绿 = 零变化证明。
- 语义级：内存后端桶分键、容量封顶、refill（既有 RateLimitEndToEndTest 覆盖路径不变）。

## §B Redis 固定窗后端与 starter 装配（T223/T224 / impl-185/186）

### Solution

- `RedisRateLimitBackend`（buzhou-store-redis，Lettuce 独占连接，AutoCloseable）：
  分钟固定窗 `INCRBY` + 首写 `EXPIRE 61s`（分钟窗 + 1s 覆盖时差）；超限即 `DECRBY`
  回滚（不泄漏额度）；TPM `consume` 只增不回滚（负余额在窗口计数上诚实表达，下窗
  自然重置）；`secondsUntilAvailable` = 当前窗剩余 + 0..50ms 微抖动（防多实例同相位
  惊群）；窗口键 = `prefix + 净化(model) + ":" + dimension + ":" + epochMinute`
  （epoch 时基——跨进程时区/时钟无关；模型名非 `[A-Za-z0-9._-]` 字符净化为 `_`，
  键结构不可注入）。
- starter 装配（T224）：`store.type=redis` **且配置任一限流容量**时 store-redis 供
  `RateLimitBackend` bean（容量 env 直读 `buzhou.resilience.rate-limit.*`，与 resilience
  配置同源；未配容量不供 bean——不白开 Lettuce 独占连接）；resilience auto-config 经
  `ObjectProvider` 优先消费共享 bean，无 bean = 内存令牌桶（默认零变化）。
  `ResilienceModule.configure` 增 backend 尾参重载；多实例 WARN 消除——共享后端下限流
  不再计入单进程告警（熔断/日配额仍告警，边界保留）。

### Implementation Decisions

- 整形特性差异诚实入档：固定窗窗口边界两窗相接可处理 2× 速率（尖峰），额度总量与
  拒绝语义同令牌桶两档等价——文档写明，不装平滑。
- 容量取 `ceil` 口径比较（INCRBY 整数计数 vs double 容量）。
- 多实例 WARN 消除路径：Redis 后端下限流不再触发「单进程」告警（熔断/日配额仍单进程，
  边界保留——见 runbook §6 改写）。

### Testing Decisions

- 语义单元级（零新依赖）：JDK 动态代理 stub RedisCommands（map 计数 + TTL 集合 +
  可注入故障）——回滚/首写 TTL/超限/预检/故障 wrap/键净化（SemanticsTest）。
- 装配级：store-redis auto-config 供 bean + destroyMethod=close；resilience
  ObjectProvider 消费优先级（既有 auto-config 测试扩展）。

## §C containers 共享额度验证（T225 / impl-186）

### Testing Decisions

- 两 backend 实例（模拟双进程，独立连接）共享同一 Redis（testcontainers redis:7-alpine）：
  RPM 容量 4 → A 扣 2 + B 扣 2 = 合并满，第 5 次任一实例拒绝；拒绝回滚不泄漏；
  模型分桶互不影响。
- TPM 跨实例记账累计（A 记 60 + B 记 40 = 满）；超额记账负余额 + 预检拒绝。
- 窗口滚动：删当前窗键模拟进下一窗（时间推进语义等价钉住）→ 全量恢复。
- 不可达 Redis fail-fast（STORE_WRITE_FAILED 带修法，不静默 fail-open）。
- `disabledWithoutDocker`：本机无 Docker 跳过，CI 具 Docker 全跑（与既有
  RedisStoresTestcontainersTest 同约定）。

## §D 红队面（T226 / impl-187）

### Testing Decisions

- 并发扣减竞差：容量 4、多线程并发 tryAcquire——放行恰 4 次（INCR 原子性 + 回滚
  净额不变，无超发、无泄漏）；并发 consume 计数不丢。
- 跨进程时钟无关性：epoch 时基窗口键在任意默认 TimeZone 下同窗同键（无本地时间
  掺入）；窗口号 = epochMillis/60000 断言钉住。
- 断连语义全路径：tryAcquire/consume/available 全部 fail-fast wrap 为 BuzhouException
  （不静默 fail-open、不吞成 0/true）。

## §E perf 哨兵 + 演示 + 文档面（T227–T229 / impl-188/189/190）

- perf 哨兵（@Tag("perf") + testcontainers）：单次 tryAcquire 往返 P95 < 50ms 硬顶
  （10 倍宽幅；container 本地预期 <5ms 量级）+ 30 连发总耗 < 1.5s。baseline 落档
  标注 container 环境口径；首轮实测值由 CI nightly 补（本机无 Docker 诚实入档）。
- 演示（examples，container 依赖标注）：双 runtime（模拟双实例）各自持有
  RedisRateLimitBackend 共享同一 Redis——第二实例的调用感知共享额度耗尽（宿主视角
  多实例协同样例）。
- 文档：runbook §6 多实例边界改写（限流从「单进程」升级「可选共享闸」+ 配置组合 +
  固定窗差异 + fail-fast 语义）；CONTEXT 术语；api-surface 快照 + 同步入档；
  新配置键无新增（复用 rate-limit.* + store.type，矩阵防线核对）。
