---
id: T6119
title: S 会话 S10 Clock-Sweep 缓存驱逐的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

缓存驱逐怎么偶发扫描不洗热页且老计数页不永驻？（spec 5009 /
effort #5009 / S10）

## Resolution

**ClockSweepCache（core/cache）**：PostgreSQL clock-sweep——
环形帧 + 时钟指针，usage=0 摘除、>0 衰减跳过（第二机会）；
命中+1 封顶 MAX_USAGE；put 覆盖不移位；确定性驱动。
