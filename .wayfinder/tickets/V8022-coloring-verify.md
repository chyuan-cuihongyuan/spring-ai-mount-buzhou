---
id: V8022
title: V 会话 V11 GraphColoring 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8021]
created: 2026-09-29
---

## Question

V11 合同怎么逐一验绿？（spec 8010 / effort #8010 / V11）

## Resolution

**验证通过**：三测全绿——偶圈/奇圈/K4/星形/独立集手锚；
200 随机图邻异色+色数 ≤Δ+1 性质；双跑确定性；fail-fast。
