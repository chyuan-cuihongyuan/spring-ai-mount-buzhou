---
id: U7203
title: U 会话 U2 Z Array Z 数组的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

前缀匹配怎么 Z-box 复用线性化？（spec 7001 / effort #7001 / U2）

## Resolution

**ZArray（core/metrics）**：z[i]=后缀与整串 LCP，
Z-box [l,r) 区间复用摊还 O(n)；p+NUL+t 一次扫描全重叠
命中；NUL 出现 fail-fast。
