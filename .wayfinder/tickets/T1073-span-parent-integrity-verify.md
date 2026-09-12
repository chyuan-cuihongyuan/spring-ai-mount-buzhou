---
id: T1073
title: span 父链完整性审计验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1072]
created: 2026-09-13
---

## Question

悬空发现与根计数如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 37 轮 = effort #736）：4 span 含 1 悬空父引用发现+根计数 1；空表/null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
