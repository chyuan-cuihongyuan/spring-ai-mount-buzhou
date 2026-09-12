---
id: T1046
title: API 快照再生 G 会话中点收口的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1045
created: 2026-09-13
---

## Question

diff 恰 8 行零意外？全量 verify 绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 48 轮）：① regenerate diff 恰 8 新增零移除；② 全仓 mvn clean verify 绿（16 模块 + 快照门 + SpecCoverage + enforcer + JaCoCo）。
