# Wayfinder Map — Buzhou 计数写路径原子化推广（effort #22）

> effort #22（已闭合 2026-08-29），延续 #5–#21；收口后累计 169 轮 / impl 1–208。
> 主线：**#16 fog 毕业生**——RunawayHook（会话级累计计数）与 TokenBudgetHook（token/
> microUsd 累计）仍是读-改-写：共享 state store 多实例下丢计数（预算被穿透）。#16 已
> 建立三栈 CAS + 进度检测重试范式——本 effort 抽公共助手并三处统一（quota/runaway/
> budget），消除第三份重复实现。

## Destination

core 新增 `AtomicStateCounters`（值形态无关的进度检测 CAS 写助手：raw→next 函数 +
停滞 16 次回退 + 回调观测）；TokenBudgetHook / RunawayHook / SessionQuotaHook 三处
计数写统一走助手；并发红队钉 runaway（beforeTool 并行扇出）与 budget（afterModel 并行
入账）不丢计数；既有 quota 并发红队回归绿；零新键。

## Notes

- 范式来源：#16（LiteLLM 原子扣减思想）——本 effort 是推广与去重，不引入新外部思想。
- 诚实边界：回退语义沿用 #16（停滞才回退、宁可少记不误拦）；fallback 观测口径
  quotaCasFallbacks 保留（resilience），core 两 Hook 以 null 回调（计数静默回退——
  core 无 stats 面，诚实入档）。

## Decisions so far

- 助手形态：`swapValue(SessionStateHandle, key, UnaryOperator<String> nextOf, Runnable
  onFallback)` 返回最终 raw 值——调用方自行 parse（值形态无关：纯数字 / day:count）。
- 三 Hook 保留各自 JVM 会话锁（默认非原子 CAS 的单实例兜底）+ 走助手 CAS。

## Not yet specified

- 观测 OLAP 增量导出 / 语义边界压缩触发 / outbox due-time 键序（fog 滚动）。

## Out of scope

- 沿用 #7–#21；core 侧 fallback 计数观测面（需求证据后议）；新配置键。

## Tickets

- [x] [T275 AtomicStateCounters 助手 + 三 Hook 统一改造](../tickets/T275-cas-helper.md)（impl-208）
- [x] [T276 并发红队（runaway 扇出 / budget 入账 / quota 回归）+ 文档面 + verify + 收口](../tickets/T276-cas-close.md)（impl-208；累计 169 轮）
