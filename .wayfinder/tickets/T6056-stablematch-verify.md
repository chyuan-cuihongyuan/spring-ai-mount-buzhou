---
id: T6056
title: R 会话 R28 稳定匹配的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6055]
created: 2026-09-23
---

## Question

R28 合同怎么逐一验绿？（spec 4027 / effort #4027 / R28）

## Resolution

**验证通过**：StableMatchingTest 六测全绿——冲突换优（A 先占 x
→ x 偏好 B 踢 A → A 转 y）；双方首选直配；不齐边 z 闲置不在
结果值；链尽者缺席；3×3 全序偏好稳定性阻塞对全扫描零命中；
畸形五型 fail-fast。首版用例两处构造缺陷（Map.of 拒 null 值、
偏好链未覆盖全受婚方致 NPE）已修。
