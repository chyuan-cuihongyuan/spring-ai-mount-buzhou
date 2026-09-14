# 1637 · 慢调用维度 yml 装配（spec 1628 配置面补全）

> 来源：N 会话 R38（effort #1637 / T2425–T2426 / impl 1190）。

## Solution

`ResilienceProperties.Circuit` 扩参 `slowCallDuration`（null/0 = 维度关）与
`slowCallRateThreshold`（缺省 0.5——与 failureRateThreshold 同默认档；∈(0,1]），
语义归位熔断组（而非顶层 record 再扩参）；`ResilienceModule` 构造 circuit 时
`withSlowCallPolicy(duration, effectiveRate)` 传导。非法值 fail-fast 带修法。

## Testing Decisions

- `CircuitSlowCallAssemblyTest` 三断言：组归一（显式 500ms/0.8）与缺省
  （null + 0.5）；非法 duration/rate fail-fast；装配语义端到端（5 次慢成功
  → OPEN——零失败前提钉住）。
- 回归：resilience 全量 396 用例。

## Out of Scope

- 慢跳闸事件的 reason 区分（payload 标 slow-call——观测细分后续轮）。
