---
id: T5022
title: Q 会话 R11 MinHash 素描的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5021]
created: 2026-09-18
---

## Question

R11 合同怎么逐一验绿？（spec 3010 / effort #3010 / R11）

## Resolution

**验证通过**：MinHashSketchTest 八测全绿——乱序同集恰 1.0（交换律
双向）、无交集恰 0.0、J=1/3 构造（8 元∩4/∪12）k=256 估计 ±0.10、
重复 offer 幂等（count 5 而相似度 1.0）、双空 NaN/单空 0、签名
96 位防御拷贝、跨实例确定性相等、0/负/不一致路数 fail-fast。
