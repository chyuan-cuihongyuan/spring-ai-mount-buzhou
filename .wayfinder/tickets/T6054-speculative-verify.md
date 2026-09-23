---
id: T6054
title: R 会话 R27 推测执行的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6053]
created: 2026-09-23
---

## Question

R27 合同怎么逐一验绿？（spec 4026 / effort #4026 / R27）

## Resolution

**验证通过**：SpeculativeStragglerPolicyTest 五测全绿——慢且落后
推测真；慢但进度 0.8 过阈豁免；290 不够慢与 300 恰界不含双豁免；
中位抗 60s 离群 + 偶数下中位 100；畸形七型 fail-fast。
