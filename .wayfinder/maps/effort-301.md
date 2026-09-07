# Wayfinder Map — Buzhou 对冲装配面（effort #301，C 会话第 2 轮）

> C 会话第 2 轮。spec 137 的 `HedgedChatModel`（gRPC hedging）是 standalone
> 原语——只有类和单测，宿主得手工 new。装配缺口：yml 声明后自动生效。

## Destination

`buzhou.resilience.hedge.*` 属性组（enabled/primary-model/model/delay）+
`@Primary buzhouHedgedChatModel` bean——开启即对冲，按名 fail-fast 解析，
默认关零行为变化。

## Notes

- 借鉴：gRPC hedging（spec 137 原始来源）；装配思想对齐 fallback/shadow 的
  按名解析 fail-fast 先例。
- 号段：本轮 spec 301 / T593–T594 / impl-324。

## Decisions so far

- 装配形态取 `@Bean @Primary` 新 bean（而非 BeanPostProcessor 换内核——
  对显式按名注入零影响，按类型注入位（含 Spring AI ChatClient.Builder）升主位）。
- 诚实边界入档：开启后宿主不得再自标 @Primary ChatModel（双主位歧义）。
- 对冲专用虚拟线程执行器独立 bean（destroyMethod=shutdown，随容器关闭）。

## Not yet specified

- 对冲计数（primary-won/fired/won）的装配级健康面（归观测族后续轮）。
- 本地（Windows）flaky 追加：resilience ShadowProbeTest.agreedAndDiverged
  （ring 完成时序序断言——单跑绿、全套偶红，与 #300 台账同性质，CI 权威）。

## Out of scope

- stream() 竞速（spec 137 显式不做）；多备对冲（单备已覆盖长尾主场景）。

## Tickets

- [x] [T593 Hedge 属性组 + @Primary 装配 bean](../tickets/T593-hedge-assembly.md)（impl-324）
- [x] [T594 装配四象限回归（默认关/装配生效/未命中 fail-fast/同名自冲拒绝）](../tickets/T594-hedge-close.md)（impl-324）
