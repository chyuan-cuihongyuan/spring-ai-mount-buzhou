# Wayfinder Map — Buzhou webhook 族组合 E2E（effort #225，B 会话第 48 轮）

> B 会话第 48 轮。B 侧 webhook 族五件（dedup 203 / redactor 177 / schema 209 /
> fanout 151 / fence 159）各自单测绿——但「叠起来是一条链」没有被证明。
> 集成轮：全链一根测试钉住组合语义（A/B 轮无此惯例，B 补上）。

## Destination

WebhookPipelineE2E（core/webhook 测试）：schema → dedup → redactor → fanout
（双 sink HTTP 收件）→ 接收方 fence 断言——一条测试证明：坏事件被拦、重复
被吞、PII 被脱、双 sink 各达、seq 连续。

## Notes

- 号段：B=奇数 spec（本轮 211）；轮次 .wayfinder200+。
- 零新生产代码——纯集成测试（组合即产品的证明）。

## Decisions so far

- 链序：schema 最外（坏事件最先拦）→ dedup（重复不浪费脱敏）→ redactor
  （出站前最后脱）→ fanout。

## Not yet specified

- 无。

## Out of scope

- 沿用各轮；性能基准。

## Tickets

- [x] [T585 全链组合 E2E 测试](../tickets/T585-pipeline-e2e.md)（impl-320）
- [x] [T586 链序语义断言（拦/吞/脱/达/序）](../tickets/T586-pipeline-semantics.md)（impl-320）
