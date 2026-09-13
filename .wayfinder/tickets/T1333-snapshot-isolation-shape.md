---
id: T1333
title: 快照数据集隔离性深验的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 48 轮：snapshotDataset（spec 187）快照隔离三断言（源变不扰靶/删源靶活/nextId 续起防碰撞）是否已有测试覆盖缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 48 轮 = effort #949 / spec 949 / impl 697）：深验成立（薄加固轮）。`SnapshotIsolationDeepTest` 三断言：① 快照后源 addItem——target items 与 fingerprint 均不变（快照是内容拷贝非活视图）；② delete source——target 存活且指纹不变（独立生命周期）；③ 快照后 target addItem——新 item id 从 max+1 续起（nextItemId 继承语义，不与拷贝项碰撞）。既有 snapshotDataset 语义零变化预期（实证出缺陷按先例修）。
