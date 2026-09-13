---
id: T1148
title: 启动阶段耗时读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1147]
created: 2026-09-13
---

## Question

时长/哨兵/幂等/升序如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 24 轮 = effort #823）：StartupPhaseTimingTest 5 例——120/45 时长精确/-1 哨兵+end 幂等 500/封顶 overflow null+升序 p0/空白忽略/clock fail-fast。
