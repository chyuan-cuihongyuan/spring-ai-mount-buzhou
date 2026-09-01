# Wayfinder Map — Buzhou 影子对照明细 JSONL 导出（effort #309，C 会话第 10 轮）

> C 会话第 10 轮。ShadowTrafficController 已发 `shadow.compared` 事件
> （payload 含 primary/shadow/耗时/token）——但明细只在事件流里过路，
> 无落盘面：离线对账/回归分析要自己接监听（fog 227「影子读对照明细
> JSONL 导出」项）。

## Destination

`buzhou.resilience.shadow.detail-path`（文件路径）声明即装配全局监听：
过滤 `shadow.compared` 事件逐条追加 JSONL（字段 at/model/primary/shadow
节选有界/耗时）；容器关闭落盘关闭。未配置 = 零变化。

## Notes

- 借鉴：W&B lineage（实验数据落盘可溯——spec 171 同族思想）。
- 号段：spec 309 / T609–T610 / impl-332。

## Decisions so far

- 消费既有事件流（零 core 改动——事件即契约）；primary/shadow 节选封顶
  1024 字符（防整文倾倒撑爆明细文件）。
- 追加写 + flush 每行（明细可 tail -f）；IO 失败吞计数不放大（旁路语义）。

## Out of scope

- 事件流全量 JSONL 化（归导出族 #344 合流打包轮一并考量）。

## Tickets

- [x] [T609 ShadowComparisonJsonl 监听器 + detail-path 装配](tickets/T609-shadow-jsonl.md)（impl-332）
- [x] [T610 导出回归（行格式/节选封顶/非目标事件忽略/IO 失败吞计）](tickets/T610-shadow-close.md)（impl-332）
