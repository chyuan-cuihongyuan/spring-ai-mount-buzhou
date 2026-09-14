---
id: T2157
title: 工具目录重名审计（ToolCatalogDuplicateAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 29 轮：工具目录重名遮蔽审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：HarnessToolCallingManager 以 HashMap 按名建索引——重名静默覆盖（后到者胜）；本地/MCP 同名是常态场景，遮蔽不可解释。

形状裁决：ToolCatalogDuplicateAudit 纯函数（core/exec）——analyze(List<String>)→Report(totalTools/distinctTools/duplicates 名字典序/duplicateCount)；DuplicateGroup(toolName/count) 仅列 ≥2；空清单哨兵；只读不裁决（行为修复另轮）。

Out of scope：fail-fast 行为变更；namespace 合成；来源分组。
