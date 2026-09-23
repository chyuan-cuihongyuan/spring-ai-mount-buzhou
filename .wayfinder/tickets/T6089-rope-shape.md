---
id: T6089
title: R 会话 R45 Rope 文本缓冲的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

大文本编辑怎么摆脱 O(n²) 全量搬移？（spec 4044 /
effort #4044 / R45）

## Resolution

**RopeBuffer（core/policy）**：xi-editor/ropey 思想——权重
平衡二叉树叶持 ≤512 字符块，charAt/insert/delete 按权重导航
split+concat；超限深度重建再平衡；length/depth 读数；越界
fail-fast。
