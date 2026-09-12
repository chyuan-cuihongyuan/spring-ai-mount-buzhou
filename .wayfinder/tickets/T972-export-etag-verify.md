---
id: T972
title: 会话导出 unchanged 协商的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T971
created: 2026-09-13
---

## Question

内容不变但导出时间不同 → UNCHANGED？任一内容字段变化 → EXPORTED + 新指纹？指纹与整体校验和可区分？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 10+1 轮）：① 同内容两次构造（exportedAt 差异）→ 指纹相等、exportIfChanged = UNCHANGED；② 改一条消息/一态 → EXPORTED + 新指纹；③ ifNoneMatch 传垃圾串 → EXPORTED（fail-open）；④ contentFingerprint ≠ of(toJson())（剔除时间戳可证）；⑤ Result/指纹快照行为稳定（两次同调同指纹）。`mvn -pl buzhou-core -am test` 全绿。
