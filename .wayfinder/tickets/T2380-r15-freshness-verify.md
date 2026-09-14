---
id: T2380
title: R15 指标新鲜度接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2379
created: 2026-09-15
---

## Question

N 会话第 15 轮：如何验收？

## Resolution

MetricFreshnessHolderTest（MutableClock）：装饰写入 touch 后 audit——active（20s 龄）
新鲜、dead（80s 龄）陈旧且年龄毫秒精确断言；未装配 audit/tracker 双 empty。
既有 MetricFreshnessTrackerTest 6 用例零回归。
