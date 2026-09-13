---
id: T1275
title: 会话导出 diff 读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 13 轮：ConfigDiff（spec 719）解决了「两份配置差在哪」——「两份会话导出差在哪」是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 13 轮 = effort #912 / spec 912 / impl 665）：缺口成立——排障场景「两份导出差在哪」（版本对比/迁移前后验证/bug 包对比）当前靠人眼逐条比 JSON。落点 core.session 新公共纯函数类 `SessionExportDiff.between(a, b)`：① 标量字段差异（version/appId/agentName——sessionId 不同 fail-fast：跨会话 diff 无意义）；② messages 按 id 对齐 → 仅 A/仅 B/两侧 content 不同三桶（有序分段保序）；③ state 键集 + value 差异；④ extensions 同 state。返回 record `DiffReport(identical, fieldDiffs, messageDiffs, stateDiffs, extensionDiffs)` 各桶有界（MAX_DIFFS=32 封顶溢出截断——719 同纪律）。纯函数零 IO；JSON 层比较（消息按 content+role 语义字段，不比时戳派生字段）。
