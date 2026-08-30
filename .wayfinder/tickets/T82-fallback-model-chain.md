---
Type: task
Status: closed
assignee: zcode
blocked-by: T81
---
## Question

备模型降级链怎么做？现状：onModelError 只能回填静态兜底文案，无备模型切换。决策点：fallback chain 配置形态（模型 bean 名列表？ChatClient 重建方式？）、触发条件（熔断 open 时直接走降级 vs 终态失败后降级）、降级后的会话内粘性（本 turn 降级 vs 后续 turn 记忆）、流式边界（M1 不做中途切换的边界如何声明）、与 T81 熔断状态的交互。产出 spec 15 增量 + impl 57。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **挂点**：降级发生在 ResilienceAdvisor 最内层内部——主模型终态失败后在**同一逻辑调用**内直接调备模型 `chatModel.call(prompt)`，外层 advisor（Hook/Memory/Observability）观察到的是一次成功逻辑调用（降级对其不可见，除事件）；不重建 ChatClient。
2. **配置形态**：`buzhou.resilience.fallback.models` = 备模型 bean 名有序列表（Spring 路径按名注入 `Map<String,ChatModel>` 解析，未命中名 fail-fast 防拼写错）；编程式路径 `ResilienceModule.configure(..., List<NamedFallbackModel>)`。
3. **触发条件**：主模型终态失败且 category ∈ `fallback.trigger-categories`（默认 `[NETWORK, SERVER, TIMEOUT, AUTH]`——AUTH 降级保可用性；CONTENT 不触发防策略跳舱；RATE_LIMIT 在外层限流器已被拒到不了这里）+ **主模型熔断 OPEN 恒触发**（CB+降级 = 主断路打开后请求零重试直达备模型，是本特性核心价值）。自限流拒绝不触发。
4. **无粘性**：每次逻辑调用独立先试主模型（熔断 OPEN 时 fail-fast 异常成本≈0）；主模型恢复（半开探测成功）自动回归——无需显式粘性状态。
5. **备模型语义**：每个备模型一次尝试（主模型已耗尽重试预算，不重复放大）；复用 deadline 执行器；各自熔断分桶独立记账（备模型也 OPEN 则跳过该级）；全败后**上抛主模型原始错误**（根因不遮蔽），备模型失败仅 WARN+事件。
6. **流式边界**：M1 流式不做降级（同「不做中途重试」边界——已发 token 不可回收）；流式仍走 onModelError 静态兜底。文档明示。
7. **观测**：事件 `fallback.switched`（from/to/category）/ `fallback.exhausted`；指标 `buzhou.resilience.fallback-switches` / `fallback-exhausted`；ResilienceStats 两计数；切换 WARNING 日志（from/to/category）。
