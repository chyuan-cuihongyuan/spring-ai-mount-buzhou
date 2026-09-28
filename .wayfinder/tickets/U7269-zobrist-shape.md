---
id: U7269
title: U 会话 U35 ZobristHashing 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

状态哈希怎么增量 O(1)？（spec 7034 / effort #7034 / U35）

## Resolution

**ZobristHashing（core/metrics）**：(位置,种类) 随机长码表+异或增量顺序无关；空位 −1 贡献 0；越域 fail-fast。
