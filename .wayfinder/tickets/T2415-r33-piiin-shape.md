---
id: T2415
title: R33 输入侧 PII 豁免的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2414
created: 2026-09-15
---

## Question

N 会话第 33 轮：输入侧豁免的会话粒度 subject 用 sessionId 还是工具式 subject？

## Resolution

选 **sessionId**（「该会话输入可信」——输入侧的信任主体是会话通道而非工具；
输出侧信任主体是工具故用 toolName）。mechanism 分侧（pii-input-redaction /
pii-redaction）——两侧豁免互不传染。类型级与输出侧同款 type:TYPE。
