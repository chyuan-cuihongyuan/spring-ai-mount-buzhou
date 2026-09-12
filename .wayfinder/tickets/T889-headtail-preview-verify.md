---
id: T889
title: 头尾预览验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T888
created: 2026-09-12
---

## Question

头尾语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（HeadTailPreviewTest 3/3 + spill 全模块 126/126 零回归）：

- 截断预览含首行与末行 + 省略标注（omitted 数与 read_range 指引）。
- 短内容原样无标注。
- JSON 数组分页预览不受影响（items/totalCount 形状不变）。
