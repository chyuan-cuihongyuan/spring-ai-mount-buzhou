---
id: T5051
title: Q 会话 R26 Rabin-Karp 的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

滑窗哈希怎么 O(1) 滚动更新且命中零误报？（spec 3025 / effort #3025 / R26）

## Resolution

**RabinKarpSearch（core/metrics，纯函数）**：滚动递推 h'=(h−左出·
B^(m−1))·B+右进（O(1)/步——全窗重哈希 O(n·m) 病的根治）+哈希
命中逐字复核（Las Vegas 零误报）+hashOf 内容哈希读数（同内容
同哈希可复算）+long 环绕隐式取模+空模式双约定与 KmpSearch 对齐。
