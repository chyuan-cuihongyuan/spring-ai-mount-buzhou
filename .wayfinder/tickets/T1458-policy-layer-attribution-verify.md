---
id: T1458
title: 策略层级归属读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1457
created: 2026-09-14
---

## Question

J 会话第 4 轮：层级归属读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（LayeredPolicyAttributionTest，AssertJ 同仓风格）：三层同键 BINDING 胜；binding 缺位 YML 补位；仅 defaults 有值 DEFAULTS；全缺 ABSENT 且 value 恒 null；嵌套点路径逐段下钻归属正确；中途遇标量（非 Map）ABSENT；**get() 对齐恒等式**——多键位上 getAttributed(key).value() == get(key)（重构等价性回归）；构造校验（ABSENT 带值 / 非 ABSENT 空值 双拒）。定向 `mvn -pl buzhou-core test -Dtest=LayeredPolicyAttributionTest` 绿 + 既有 LayeredPolicyTest 回归绿。
