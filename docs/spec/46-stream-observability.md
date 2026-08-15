# Spec 46 — 流式体验可观测与终止语义（effort #10）

> effort #10 首篇。§A：TTFT/TPOT 流式指标（T170）；§B：流取消分类计数与慢滴流累计上限（T171）。
> 外部事实源：LiteLLM（~26K★）`time_to_first_token` / `latency_per_output_token`；
> vLLM（~45K★）`time_to_first_token_seconds` / `time_per_output_token_seconds` 直方图——
> 两家均无流取消一级指标（真空区，本篇补齐）。语义借鉴零新依赖。

## §A TTFT/TPOT 流式指标（T170 / impl-139）

### Problem Statement

流式回复的用户体验核心指标是「多久出第一个字」（TTFT）与「后续吐字节奏」（TPOT）。当前
adviseStream 只在流终结时聚合 usage 与正文，首信号无打点——流式劣化（排队长、模型慢）在指标面
不可见，只能靠端到端投诉定位。

### Solution

流式路径在 MODEL_CALL span 上补三个观测点：首内容信号时刻（TTFT）、每输出 token 均摊耗时
（TPOT，仅在 completion tokens > 1 时定义）、两者作为 span 属性与 micrometer timer 落档。
非流式路径不产出这两项指标（语义不存在）。

### User Stories

1. As a 运维工程师, I want `buzhou.model.ttft` 直方图, so that 流式首字劣化在告警面可见。
2. As a 运维工程师, I want `buzhou.model.tpot` 直方图, so that 吐字节奏劣化（模型过载典型症状）可与其他慢区分。
3. As a 平台用户, I want 回放界面看到每次模型调用的 ttft.ms/tpot.ms, so that 「感觉慢」可定位到首字慢还是吐字慢。
4. As a 框架开发者, I want 指标预注册（启动零值可见）, so that 面板序列从启动起完整。
5. As a 框架开发者, I want 无首内容的流（错误/取消先至）不记录 TTFT, so that 指标语义纯净不被污染。

### Implementation Decisions

- 「首内容信号」定义：流中首个使正文/思维链累计器从空变非空的信号，或首个携带工具调用 delta 的信号
  ——usage-only / role-only 空块不计（诚实口径：首个有内容到达订阅者的时刻）。
- TTFT 计时起点：adviseStream 订阅建立时刻（doOnSubscribe 记 nanoTime）。
- 记录点：首内容信号时——MODEL_CALL span 属性 `ttft.ms`；micrometer timer `buzhou.model.ttft`
  （tag：model.name，记录侧截断 64，与 model.call.duration 同纪律）。
- TPOT：流完成时若 lastUsage.completionTokens > 1，`(总时长 − TTFT) / (completionTokens − 1)`，
  span 属性 `tpot.ms` + timer `buzhou.model.tpot`（同 tag 纪律）；TTFT 缺失（无首内容）则 TPOT 不记。
- 新增 EventType 内置常量 `STREAM_FIRST_TOKEN`（开放枚举注册表语义不变），事件 payload 携带
  `ttft.ms`——事件面与 dashboard 回放可用。
- BuzhouMetricsBinder 预注册两 timer 无 tag 基型（与 resilience 族同法）；未装 micrometer 零开销。
- 非流式 adviseCall 路径零改动。

### Testing Decisions

- 观测模块既有端到端测试模式（ObservabilityEndToEndTest）：伪流式 ChatResponse 序列 →
  断言 span 属性 ttft.ms 存在且量级合理、STREAM_FIRST_TOKEN 事件恰一条、先空块后内容块的序列
  TTFT 计的是内容块时刻。
- meter 断言经 BuzhouMetricsHolder 测试探针（既有模式）；TPOT：两 token 流（completion=2）断言
  (总−TTFT)/1 量级；completion ≤ 1 或无 TTFT 断言不记。

### Out of Scope

- 降级链按「首 token 已发出」区分失败成本（记入 T186 后续评估）。
- TTFT 分位数告警阈值（运维侧配置，不入框架）。

## §B 流取消分类计数与慢滴流累计上限（T171 / impl-140）

### Problem Statement

流终止原因在指标面不可分：订阅者主动取消、超时截断、护栏拦截共用零观测，运维无法回答「流式失败里
多少是客户端断开、多少是超时」。另一缺口：流式 timeout 为「相邻信号间隔」语义——每 9s 滴一个字的
慢滴流永不触发截断（DefaultAgentSession 注释自认），一轮可被无限拖长。

### Solution

终止原因分类计数器 `buzhou.stream.cancelled{reason=client|deadline|guard}`（有界枚举、预注册）；
新增流累计时长上限 `buzhou.core.stream-total-timeout`（缺省 10m，≤0 关闭；实现期自
`buzhou.session.*` 修正为 `buzhou.core.*`——与 tool-timeout 等既有 core 旋钮同族）——自订阅起计，
超限以错误终结流并按 reason=deadline 计数，走既有 failTurnOnce 收尾路径。

### User Stories

1. As a 运维工程师, I want 取消原因分类计数, so that 「流式成功率」可拆解为客户端行为与框架截断。
2. As a 运维工程师, I want 慢滴流被累计上限截断, so that 单轮资源占用有硬顶、租约不被慢性消耗拖垮。
3. As a 宿主开发者, I want 累计上限可配且能关闭, so that 特殊长流场景（慢速生成告示）有逃生舱。
4. As a 平台用户, I want 超限错误带明确文案, so that 知道是框架截断而非模型故障。

### Implementation Decisions

- 计数落点（core，DefaultAgentSession）：订阅者 cancel（doFinally CANCEL 路径）→ client；
  流超时/累计超限错误（doOnError 路径，含 timeout 算子 TimeoutException 与累计上限标记异常）→
  deadline；beforeTurn 护栏拦截（返回 Flux.error 前）→ guard。计数经 BuzhouMetricsHolder。
- 累计上限实现：流包装 `takeUntilOther(Mono.delay(cap).then(Mono.error(标记异常)))` 语义——
  超限时流以标记异常 onError 终结，复用既有 doOnError→failTurnOnce 链路（TURN 记账/span 关闭
  均既有语义）；标记异常为 core 内部类型，消息注明「流累计时长超限」。
- 配置：`BuzhouCoreProperties.Core` 新键 `stream-total-timeout`（Duration，缺省 10m；≤0 显式关闭），
  `HarnessAssembler.withStreamTotalTimeout` wither 传入 `DefaultAgentSession` 新构造参数（既有构造兼容重载保留）。
- 诚实边界：累计超限触发时上游（含 ObservabilityAdvisor 的 span 包装）收到的是 cancel 信号——
  MODEL_CALL span 终态可能记为 CANCELLED 而非 ERROR；TURN 级记账（failTurnOnce + 标记异常 + deadline
  计数）不受影响，准确。
- 相邻信号间隔 timeout（既有）语义不变；两道上限并存时先到者生效。
- 行为变更入档：缺省开启 10m 上限（此前语义为不限），pre-1.0 允许，api-surface 记录。

### Testing Decisions

- 取消分类（core）：伪流 + 三路终止各断言计数 +1 且互不串扰（client=dispose、deadline=不产出信号的
  Flux + 短 turnBudget、guard=beforeTurn 拦截）。
- 慢滴流（core）：注入式「每 tick 滴一字」伪流 + stream-total-timeout 设 200ms → 到点以标记异常
  终结、reason=deadline 计数、onTurnError 通知到达。
- 关闭开关（≤0）回归：长流不被截断（既有语义）。

### Out of Scope

- 取消原因进 OTel span 属性（advisor 层拿不到原因，涉及跨模块通道扩面；T186 再评估）。
- 每会话并发流计数治理（单飞闸已覆盖并发面）。
