---
id: T2627
title: 工具参数形态分布的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ToolArgShapeAudit 的形状怎么裁决？（spec 1713 / effort #1713 / R14）（spec 1713 验收/裁决）

## Resolution

七态闭集 Shape（EMPTY/JSON_OBJECT/JSON_ARRAY/NUMERIC/BOOLEAN/LARGE_BLOB/PLAIN_TEXT）+实例面分类计数+census()；blob 判定先于 JSON（超长不解析防御优先）；阈值默认 4096 可调；classify 静态纯函数供探测——jq 类型分诊思想。
