# Spec 49 — Shadow 探测与池级配额（effort #10）

> effort #10 第四篇。§A：shadow fork 探测（T176）；§B：模型池配额全候选执行（T177）。
> 外部事实源：LiteLLM Router（~26K★）shadow 流量经 eval 工具叠加 + per-deployment tpm/rpm；
> 本地勘察证实既有 ModelRateLimiter 已按模型分桶（RPM+TPM、预检+记账），但 fallback/canary
> 候选调用完全绕过限流——§B 是执行面补全而非新机制。

## §A shadow fork 探测（T176 / impl-145）

### Problem Statement

换模型决策只有「切过去看」一条路：金丝雀只能测真实用户体验，无法在零用户影响下对照
主/备模型对同一输入的延迟与 token 差异。

### Solution

`buzhou.resilience.shadow.*` 配置开关（默认关）：主模型调用**成功后**异步把同一 Prompt 发给
shadow 模型（裸 ChatModel 调用——**不重放工具循环**，杜绝工具副作用双跑）；结果不回注用户，
仅发 `shadow.compared` 对照事件（primary/shadow 延迟与差值）+ 计数器。并发与预算护栏：
进程级并发信号量 + UTC 日预算池（池尽即停，可观测不静默）。

### User Stories

1. As a 平台工程师, I want 同一输入的主/备模型延迟对照, so that 换模型决策有数据依据。
2. As a 平台用户, I want shadow 绝不影响我的回复与延迟, so that 探测对我零感知。
3. As a 运维工程师, I want shadow 有日预算与并发上限, so that 探测成本有硬顶、不挤占生产配额。
4. As a 框架开发者, I want shadow 失败只计数不上抛, so that 探测故障绝不拖垮主链路。

### Implementation Decisions

- 配置组 `ResilienceProperties.Shadow`（`buzhou.resilience.shadow.*`）：`enabled`（默认 false）、
  `models`（ChatModel bean 名列表，未命中启动失败——与 fallback 同 fail-fast 口径）、
  `max-concurrent`（默认 2）、`daily-budget`（默认 1000，进程级 UTC 日池）。
- 执行面 `ShadowTrafficController`（进程级共享）：submit(prompt, primaryName, primaryLatencyMs,
  emitter)——护栏检查（并发 tryAcquire / 日预算原子扣减）通过后虚拟线程顺序调用各 shadow 模型，
  计时 + usage 提取，发 `shadow.compared` 事件（payload：primary/shadow/primaryMs/shadowMs/deltaMs）
  + `buzhou.resilience.shadow.calls{outcome}` 计数；护栏拦下计 `skipped-concurrency`/
  `skipped-budget`；shadow 自身异常计 `error` 吞掉（DEBUG 日志，不上抛）。
- 挂点：ResilienceAdvisor 主路径成功后（doAdviseCall 返回与延迟测量之间，返回前提交——
  提交即返回，用户路径零增延迟）；金丝雀路径与流式路径**不探测**（诚实边界：金丝雀目标本身
  即候选在测；流式聚合语义与裸调用不可比）。
- **工具副作用红线**：shadow 只做裸 ChatModel.call(prompt)——prompt 是数据快照，无工具绑定；
  绝不重放 advisor 链/工具循环。
- 未启用（默认）：零提交、零事件、零计数。

### Testing Decisions

- 对照事件：primary ok + shadow 固定回复 → shadow.compared 事件（deltaMs 数值合理）+ 用户回复
  不受影响；异步面用轮询（有界超时）等事件。
- 预算：dailyBudget=1 → 第二次提交计 skipped-budget、无事件。
- 并发：maxConcurrent=1 + 慢 shadow（闭锁挂住）→ 并发第二次提交计 skipped-concurrency。
- 失败吞噬：shadow 抛错 → outcome=error 计数、主链路回复照常。
- 默认关：无配置零事件零计数。

### Out of Scope

- shadow 结果自动回流评估集（与 T174 导出面衔接属后续；本篇只产事件）。
- shadow 流式（流式聚合与裸调用不可比，边界入档）。

## §B 模型池配额全候选执行（T177 / impl-146）

### Problem Statement

ModelRateLimiter 按模型分桶（RPM+TPM、调用前预检/调用后记账），但只有主模型经 RateLimitAdvisor
过闸——fallback / 金丝雀 / shadow 的模型调用完全绕过限流：备模型被打爆无闸，池级配额名存实亡。

### Solution

降级链全部候选调用统一过限流闸：ResilienceAdvisor 的 fallback / 金丝雀回退候选调用前
`acquireOrThrow(candidate)`、成功后 `recordUsage(candidate, usage)`；限流拒绝视作该候选失败
（跳下一候选，不视作模型故障入熔断窗）。remaining 水位经 gauge 家族可观测。

### User Stories

1. As a 运维工程师, I want 备模型也受限流闸保护, so that 降级风暴不打爆备模型配额。
2. As a 运维工程师, I want 各模型桶 remaining 水位可观测, so that 配额调优有数据。
3. As a 平台工程师, I want 限流拒绝不误伤熔断窗, so that 配额问题不污染故障率信号。

### Implementation Decisions

- ResilienceAdvisor 构造增可选 `ModelRateLimiter` 引用（null = 未配置，既有行为）；候选循环
  （fallbackOrRethrow / degradeFromCanary）每候选：调用前 `acquireOrThrow(name, emitter)`
  （`ModelRateLimitExceededException` → 计数 `skipped-ratelimit`、跳下一候选、不入熔断窗口）、
  成功后 `recordUsage(name, totalTokens, emitter)`（usage 从响应元数据提取；缺失记 0 留痕
  ——沿既有 `backpressure.model-usage-missing` 事件口径）。
- RateLimitAdvisor（主模型闸）位置不动（+650 外层）；候选闸在 resilience 层内嵌。
- remaining 可观测：ModelRateLimiter 增 `remainingRatio(modelName, dimension)` 只读探针
  （gauge 注册由模块装配完成，`buzhou.resilience.ratelimit.remaining{dimension}`——按已知
  模型名集合注册，避免 gauge 基数无界）。
- 诚实边界沿用：限流桶时间行为不注入 Clock（effort #9 T154 已钉边界）；测试用小容量桶。

### Testing Decisions

- 候选过闸：primary 失败 → 备模型成功 → limiter 记账（usage 计入备模型桶）；
  备模型桶打空 → 跳过该候选到下一级（skipped-ratelimit 计数、熔断窗零样本）；
- remaining 探针：消耗后 ratio 单调下降（0..1）。
- 回归：无 limiter 配置时降级行为逐字节不变。

### Out of Scope

- 主模型闸与候选闸的合并（RateLimitAdvisor 内移属 advisor 序重构，量级不抵）。
- 按租户/应用维度的配额（单 Agent 运行时边界外）。
