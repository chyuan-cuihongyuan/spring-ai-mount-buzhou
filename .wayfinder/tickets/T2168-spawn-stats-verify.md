---
id: T2168
title: spawn 漏斗守恒与峰值水位的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2167
created: 2026-09-14
---

## Question

如何证明漏斗计数、守恒式与峰值水位？

## Resolution

**用户常设授权 AFK（可推翻）**

`SessionSpawnStatsTest` 三测全绿（`mvn -pl buzhou-core -am test`，E2E 走真实 runtime）：双 spawn 成功计数+峰值≥2；同 id 二次 spawn 冲突计数+守恒闭合；**steal 路径**（SpawnOptions.withSteal）计数且计入 successes。静态面前后归零防串扰。评审修正：record 方法初版包私有跨包不可见（R14 同款教训）改 public。
