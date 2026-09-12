# Wayfinder Map — Buzhou guard 装配摘要读数（effort #549，E 会话第 49 轮）

> E 会话第 49 轮（排障可观测小轮）。勘察：guard 装配了哪些 hook 只能
> 翻 builder 代码——「guard 到底挂了哪些钩子」无读数面（支持包/排障
> 第一问）。

## Destination

GuardModule.assemblySummary()：hook 名列表（装配序）——支持包/排障
一屏可读；未知 yml 键宽容忽略（错键治理归 config doctor 面）。

## Notes

- 号段：spec 549 / T859-860 / impl-451。
- 无新顶层公共类型（加法方法）。

## Out of scope

- 未知键 WARN/doctor 化（后续轮）；per-hook 配置详情。

## Tickets

- [x] [T859 assemblySummary](../tickets/T859-assembly-summary.md)
- [x] [T860 未知键宽容](../tickets/T860-unknown-keys-tolerant.md)
