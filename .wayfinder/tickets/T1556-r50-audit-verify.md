---
id: T1556
title: R50 周期预检轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1555
created: 2026-09-14
---

## Question

J 会话第 50 轮：预检如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：隔离 worktree（/tmp/j50-verify，HEAD=82e9c0be 干净基线）全仓 `mvn verify` 单命令——编译 + 全模块测试 + JaCoCo LINE ≥70% + enforcer 依赖收敛 + SpecCoverageTest（README↔spec 双向引用门）+ ApiSurfaceSnapshot（顶层类型快照门；R46–R49 新增均为嵌套 record，顶层类型集合不变应免于快照破坏）。BUILD SUCCESS + 14 模块 SUCCESS + 双门绿即验收。
