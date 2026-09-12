# Wayfinder Map — Buzhou 事故复盘一键包（effort #521，E 会话第 22 轮）

> E 会话第 22 轮（317 ExportBundle 事故域预设组合扩散轮）。勘察：复盘
> 要手工凑三面数据（405 时间线/83 错误签名/334 成本 rollup）——一键
> 标准打包空白；317 ExportBundle 已提供 ZIP+manifest 对账（单源故障
> 隔离）。

## Destination

`export.PostmortemBundle`（builder 流式装配三可选源）：compose(zip) →
标准命名源打包（postmortem.timeline.jsonl / postmortem.error-signatures
.jsonl / postmortem.cost-model|virtual-key.jsonl / postmortem.summary.json
汇总行）；源缺席跳过不中断（317 同语义）；复用 ExportBundle.bundle
（manifest 对账免费获得）。

## Notes

- 号段：spec 521 / T793–T794 / impl-424。
- 借鉴源：PagerDuty incident 附件包 / 317 OCI artifact 同族。

## Out of scope

- 采集新数据（只聚合既有观测面快照）；自动触发（事故判定归 312/321）。

## Tickets

- [x] [T793 标准复盘源组合](../tickets/T793-postmortem-compose.md)
- [x] [T794 缺席源跳过与 summary](../tickets/T794-postmortem-absent.md)
