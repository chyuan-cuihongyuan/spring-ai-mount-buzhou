---
id: T1017
title: 导出协商联动补验的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

SessionExportConditional 单元面已验——与 toJson/fromJson 往返、整体校验和的组合链路未闭环。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 34 轮 = effort #733 / spec 733 / impl 536，测试域补验轮）：三组组合用例——① toJson→fromJson 往返后 contentFingerprint 稳定；② 两轮协商周期（UNCHANGED → 内容变 → EXPORTED 新指纹）；③ 双校验和各自幂等。
