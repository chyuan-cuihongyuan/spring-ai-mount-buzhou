---
id: T5029
title: Q 会话 R15 KMP 搜索的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

精确子串匹配怎么免朴素回退且可重叠全命中？（spec 3014 / effort #3014 / R15）

## Resolution

**KmpSearch（core/metrics，纯函数）**：lps 失配函数（最长真前后缀）
预构+失配滑模式不回退主指针（O(n+m)——朴素 O(n·m) 回退病的根治）
+indexOf JDK 同款空模式约定+findAll 可重叠（命中回退 lps 前缀继续
——空模式诚实拒绝）+failureFunction 公共读数（对账面）。
