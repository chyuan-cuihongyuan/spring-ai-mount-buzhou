---
id: T918
title: 时间旅行 fork 回放起点落 state 的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

spec 602 谱系的 upToTurn 只在事件 payload——state 面查不到「从第几轮重走」。补吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 35 轮 = effort #600 / spec 634 / impl 487）：

1. forkFromTurn 另写 `buzhou.fork.turn`（value=轮次字符串，producer 同 fork）——普通 fork 不写（无起点语义）。
2. 与 buzhou.fork.source 同生命周期（无 TTL、导出/导入携带）；面板/模块可查完整「fork 自谁+从哪重走」。
