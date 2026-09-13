---
id: T1147
title: 启动阶段耗时读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

启动计时做进 lifecycle 还是独立读数面？未结束步骤语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 24 轮 = effort #823 / spec 823 / impl 576）：`StartupPhaseTiming` 独立读数面（lifecycle 零侵入）——start/end 句柄+volatile 首末幂等+未结束 -1 哨兵+升序快照+封顶 64；喂点归应用侧。
