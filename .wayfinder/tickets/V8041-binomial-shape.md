---
id: V8041
title: V 会话 V21 BinomialHeap 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

堆合并怎么二进制进位？（spec 8020 / effort #8020 / V21）

## Resolution

**BinomialHeap（core/concurrent）**：度互异二项树森林+按
度进位合并+poll 摘根孩子反序回并；森林审计面（度互异+
节点数守恒）。（勘误：原拟 FibonacciHeap 退雾区——U24 同
款纪律，同族二项堆补位。）
