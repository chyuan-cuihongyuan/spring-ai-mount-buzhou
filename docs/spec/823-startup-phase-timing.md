# 823 — 启动阶段耗时读数

> 来源：H 会话第 24 轮 = effort #823 / [T1147](../../.wayfinder/tickets/T1147-startup-phase-timing.md) / [T1148](../../.wayfinder/tickets/T1148-startup-phase-timing-verify.md) / impl 576。
> 借鉴：Spring Boot ApplicationStartup（≈78K star）。

## Problem

应用启动慢时只能翻日志猜阶段边界：装配期各机制（stores/hook 链/预热）的耗时没有结构化留痕。

## Solution

`StartupPhaseTiming`（core.config）：

- **步骤句柄**：start(phase) → Step → end()（volatile 首末幂等）；Clock 注入可测。
- **快照**：升序 StepTiming 列表；未结束 duration=-1 哨兵。
- **有界**：封顶 64 步（超丢+truncated）；空白 phase 忽略。

## 兼容性

纯新增读数面（喂点归应用侧）；零装配侵入。

## 诚实边界

手动喂点不自动埋点；毫秒粒度；一次性语义无聚合。
