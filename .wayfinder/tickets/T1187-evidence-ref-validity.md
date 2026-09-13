---
id: T1187
title: 证据引用失效率读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

引用断链对账做进 ledger 还是独立纯函数？存在性语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 44 轮 = effort #843 / spec 843 / impl 596）：`EvidenceRefValidity` 独立纯函数（ledger 包私有边界保持）——existence 谓词注入+失效率+样本典序封顶 16；null/空白忽略；空真。
