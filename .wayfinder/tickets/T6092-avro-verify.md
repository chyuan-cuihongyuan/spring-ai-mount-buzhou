---
id: T6092
title: R 会话 R46 Avro 读写模式解析的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6091]
created: 2026-09-24
---

## Question

R46 合同怎么逐一验绿？（spec 4045 / effort #4045 / R46）

## Resolution

**验证通过**：SchemaEvolutionTest 七测全绿——同型直读/三条
加宽链/不兼容 fail-fast 带字段名/默认补位 vs 无默认 fail-fast/
alias 配对/dropped 显形/确定性回放。
