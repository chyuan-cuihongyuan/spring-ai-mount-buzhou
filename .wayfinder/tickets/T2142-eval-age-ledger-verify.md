---
id: T2142
title: 运行年龄台账哨兵与埋点接线的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2141
created: 2026-09-14
---

## Question

如何证明埋点接线、年龄哨兵与最长完成水位？

## Resolution

**用户常设授权 AFK（可推翻）**

测试计划：直调 recordOpened/recordClosed 断言快照（无活跃 -1 哨兵/最老年龄=最早开启者/最长完成水位）；Registry E2E：begin→close 循环（新实例 install）断言 closed 计数与 age 落窗；reset 归零。静态面前后归零防串扰。
