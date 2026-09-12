# 647 — hook 计时进程级聚合与健康读面

> 来源：F 会话第 48 轮 = effort #600（spec 646 装配后补全——生效读面模式）/ [T944](../../.wayfinder/tickets/T944-hook-aggregate-shape.md) / [T945](../../.wayfinder/tickets/T945-hook-aggregate-verify.md) / impl 500。

## 背景

spec 646 的 per-hook 计时随 HookChain 实例（每会话一条）私有——「全进程哪个 hook 最慢 / 吃掉多少内联预算」需要跨会话拼装；观测价值集中在进程级。

## 目标

- `HookTimingAggregator`（core.hook，Holder 模式——RetryBudgetHolder 同款）：`ConcurrentHashMap<hookName, Timing>` 进程级聚合；HookChain 计时点在 aggregator 开启时镜像累计（链内私有 stats() 口径不变——零观测语义变化）。
- Spring 装配默认开启聚合（`BuzhouCoreAutoConfiguration`）+ `HookTimingHealth implements BuzhouHealth`：UP + details = 每 hook `{count, totalMicros, maxMicros, avgMicros}`（ErrorSignaturesHealth 同构范式；`/actuator/buzhou-health` 族可扫读）。
- 编程式 / 未装配：aggregator 关 = 纯私有计时，零变化。

## 非目标

不改 spec 646 的链内 stats() 语义；不做阈值 yml 化（沿 646 非目标）。

## 测试

跨链同名合并（aggregator 开）；缺省零共享（既有计时用例零回归）；装配后健康面 details 与聚合一致。

## 兼容性

HookChain 公共签名不变；纯增量聚合与读面。
