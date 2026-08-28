# Spec 62 — 计数写路径原子化推广（effort #22）

> wayfinder map：`.wayfinder22/MAP.md`（T275–T276）。范式来源：spec 56（#16）；
> 本 effort 为推广与去重。

## Problem Statement

RunawayHook 的会话级累计计数与 TokenBudgetHook 的 token/microUsd 累计仍是
读-改-写：多实例共享 state store 时并发写互相覆盖——计数少记，预算硬顶被穿透。
spec 56 已为配额建立三栈 CAS + 进度检测重试范式，但逻辑内联在 SessionQuotaHook，
推广需复制第三份。

## Solution

core 抽 `AtomicStateCounters.swapValue(handle, key, nextOf, onFallback)`：值形态无关
（调用方给定 raw→next 函数）的进度检测 CAS 写——值在变即续试、停滞 16 次回退覆写 +
回调观测。TokenBudgetHook / RunawayHook / SessionQuotaHook 三处计数统一走助手；
并发红队钉 runaway 工具扇出与 budget 入账不丢计数。

## User Stories

1. 作为多实例运维者，我要 runaway 会话计数跨实例原子累计，所以失控防护上限不被并发穿透。
2. 作为多实例运维者，我要 budget 的 token/microUsd 累计原子，所以成本硬顶不可绕过。
3. 作为 SDK 开发者，我要一个可复用的 CAS 计数助手，所以第四个计数场景不再复制粘贴。
4. 作为红队，我要并行扇出/入账的终值精确等于次数×增量，所以丢更新被钉死。
5. 作为既有用户，我要单进程行为零变化，所以升级零风险。

## Implementation Decisions

- 助手在 core.internal.hook（非公共面——Hook 内部实现细节）。
- 三 Hook 保留 JVM 会话锁（单实例兜底）+ 助手 CAS（跨实例正确性）。
- quota 的 quotaCasFallbacks 观测保留（回调注入）；core 两 Hook 回调 null（静默回退入档）。

## Testing Decisions

- 并发红队：多线程驱动 beforeTool/afterModel 断言终值；quota 既有红队回归。

## Out of Scope

- core 侧 fallback 计数观测面；新配置键。

## Further Notes

- 回退语义与 spec 56 完全一致（停滞才回退——运行中不触发）。
