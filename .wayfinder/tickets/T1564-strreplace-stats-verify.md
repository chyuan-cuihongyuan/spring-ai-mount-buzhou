---
id: T1564
title: str_replace 编辑判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1563
created: 2026-09-15
---

## Question

J 会话第 54 轮：StrReplaceStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（StrReplaceStatsTest，TempDir 骨架）：唯一命中替换成功 → successes=1；缺 newStr 与空 oldStr → paramRejects=2；文件不存在 → missingFileRejects=1；oldStr 未出现 → notFoundRejects=1；oldStr 多处 → ambiguousRejects=1；混合守恒 attempts = successes + 五拒绝桶；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='StrReplaceStatsTest'` 绿 + 既有 StrReplaceTool 回归绿。
