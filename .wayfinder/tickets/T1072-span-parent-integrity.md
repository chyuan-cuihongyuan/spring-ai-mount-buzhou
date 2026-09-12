---
id: T1072
title: span 父链完整性审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

父 span 被逐出/未落库→子 span 悬空、trace 树断裂——加父链审计吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 37 轮 = effort #736 / spec 736 / impl 636）：`SpanParentIntegrityAudit.audit(List<SpanRecord>)` 纯函数——集合内 spanId 索引判悬空父引用 Finding+totalSpans/rootSpans；无父=根合法；调用方供单会话/单 trace 集合。环不做（记录不可变不构成）。
