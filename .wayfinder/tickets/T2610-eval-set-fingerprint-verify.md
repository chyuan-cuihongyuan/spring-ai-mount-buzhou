---
id: T2610
title: 评测集内容指纹的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2609
created: 2026-09-15
---

## Question

EvalSetFingerprint 怎么验证？（spec 1704 验收）

## Resolution

`EvalSetFingerprintTest`（core，纯函数直测）：格式前缀+64hex+确定性；ORDERED
对换序敏感/UNORDERED 不敏感；单字变化与重复项变化即变；null 与空表同指纹。
