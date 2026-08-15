# Spec 44 — 停机排空与观测纪律（effort #9）

> effort #9 主线 spec。§A：停机排空补全（T159）；§B：观测纪律收口（T160）。

## §A 停机排空补全（T159 / impl-130）

### Problem Statement

两处停机语义缺口：`SleepTimeScheduler.close()` 直接 `shutdownNow()`——pending 整理任务被丢弃
（MemoryModuleLifecycle javadoc 自认「优雅排空属后续切片」的遗留承诺）；`WebhookEventForwarder`
close 排空预算硬编码 5s 且无直接测试钉住（排空行为回归不可检）。

### Solution

SleepTimeScheduler 优雅关闭：先 `shutdown()` 排空在途整理任务（有界预算，缺省 5s、构造可调），
超时再 `shutdownNow()` 硬截断。Webhook close 排空预算可配（`buzhou.webhook.close-drain-timeout`，
缺省 5s）并以确定性测试钉住「close 等待在途投递收尾并排空已到期记录」。

### User Stories

1. As a 运维工程师, I want 停机时 pending 整理任务有界排空, so that 计划内的重启不丢摘要整理工作。
2. As a 运维工程师, I want webhook 停机排空预算可配, so that 与容器终止宽限期对齐。
3. As a 框架开发者, I want close 排空语义被测试钉住, so that 回归可检。

### Implementation Decisions

- `SleepTimeScheduler.close()`：shutdown → awaitTermination(closeGrace) → 超时 shutdownNow；
  四参构造增 closeGrace（非正回退 5s）。
- `BuzhouWebhookProperties` 增 `closeDrainTimeout`（7 参；6 参兼容构造保留、canonical
  @ConstructorBinding）；`effectiveCloseDrainTimeout()` 生效值（null/非正 → 5s；非正配置 fail-fast）。
- `WebhookEventForwarder.close()/drainDueBeforeClose()` 改用生效预算；未到期退避记录留存语义不变。

### Testing Decisions

- webhook close 排空（core，JDK HttpServer 挂起收件方）：A 在途挂住 + B 已到期排队 → close 有界
  等待 A 收尾并排空 B → delivered=2、pending=0。
- scheduler（memory）：预算内任务不被中断丢弃完成；预算外任务 close 有界返回（硬截断）。

### Out of Scope

- 跨重启的整理任务持久化队列（pending 属进程内工作集，语义上允许丢失——本片只消除「计划内停机
  也立即丢弃」的粗放）。

## §B 观测纪律收口（T160 / impl-131）

### Problem Statement

指标 tag 基数纪律只落了一半：RateLimitAdvisor 截模型名 32 字符，但 ModelCircuitBreaker 的两个
gauge 与 circuit-tripped/rejected 计数器、ResilienceAdvisor 的 fallback-switches（from/to）直接
用原始模型名——依赖「模型名集合有界」的隐含假设。依赖治理同理：enforcer 只有内部模块版本对齐，
三方依赖版本分歧（Maven 最近者胜出）无门禁。

### Solution

tag 截断抽公用工具（MetricTags.bound，32 字符，null/blank → unknown），全部模型名 tag 落点统一
走工具；enforcer 增 dependencyConvergence + banDuplicatePomDependencyVersions 硬门禁，既有分歧
（json-schema-validator 3.0.0/3.0.1）经 dependencyManagement 显式钉 3.0.1 收口。

### User Stories

1. As a SRE, I want 模型名 tag 有统一长度上限, so that 异常长的模型标识不会撑爆指标基数。
2. As a 框架开发者, I want 三方依赖版本分歧被 CI 拒绝, so that「最近者胜出」的静默漂移不再可能。

### Implementation Decisions

- `MetricTags`（resilience 公共小工具）：bound(value) 截 32；RateLimitAdvisor 既有私有法委派之。
- 落点：ModelCircuitBreaker（circuit-open / backoff-multiplier gauge + tripped/rejected 计数）、
  ResilienceAdvisor（fallback-switches from/to）。
- enforcer 第二执行段 `enforce-dependency-governance`：dependencyConvergence +
  banDuplicatePomDependencyVersions；json-schema-validator 钉 3.0.1（Spring AI 双路传递分歧，
  取高者，入档注释）。

### Testing Decisions

- MetricTags 单测（截断/null/blank）；门禁以本仓全模块 enforcer 通过为验收。

### Out of Scope

- Prometheus register 侧基数上限（部署域）。
