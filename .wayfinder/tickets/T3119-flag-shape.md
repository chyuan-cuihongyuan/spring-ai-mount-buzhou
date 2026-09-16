---
id: T3119
title: flag 求值错误语义的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

特性开关求值的错误兜底与病灶显形怎么定语义？（spec 2009 / effort #2009 / R10）

## Resolution

**OpenFeature 线程安全求值器 `FlagEvaluator`（core/policy）**：
FlagDefinition record（默认变体+targeting/命中变体成对守约）+
evaluate 永不抛出（未注册 FLAG_NOT_FOUND 空值/命中 TARGETING_MATCH/
未命中 STATIC/谓词炸 DEFAULT 兜底且 ERROR 同记）+ reasonCounts 五态
分布（幽灵 flag 与谓词病灶分别显形）+ registeredFlags 注册面。
