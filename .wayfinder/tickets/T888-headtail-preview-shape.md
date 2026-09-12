---
id: T888
title: spill 预览头尾语义的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

spill 预览（默认 2048 字符）纯头截断——大结果的关键信息常在尾部（汇总行/结论/总计数），模型只见开头。头尾语义怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 20 轮 = effort #600 / spec 619 / impl 472）：

1. 截断时预览 = 头 3/4 + 「…（中间省略 N 字符，read_range 可回读）…」标注 + 尾 1/4（ripgrep context 语义——收尾优先可见）。
2. 预算量级不变（正文 ≤ previewChars + 标注小开销）；短内容原样；JSON 数组分页预览路径不受影响（列表走 readPage 首页，带 totalCount 已是全貌口径）。
