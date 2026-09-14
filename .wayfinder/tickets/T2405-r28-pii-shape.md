---
id: T2405
title: R28 PII 豁免双粒度的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2404
created: 2026-09-15
---

## Question

N 会话第 28 轮：PII 豁免粒度选哪种？

## Resolution

选 **双粒度（工具级 + 类型级）**。「规则误报」是类型维度痛点、「可信数据源」
是工具维度痛点——单粒度必弃一方。类型级实现为生效集剔除（detector.redact 的
类型参数过滤）而非命中后回填——语义干净无回退窗口。
