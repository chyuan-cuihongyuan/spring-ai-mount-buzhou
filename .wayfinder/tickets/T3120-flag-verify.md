---
id: T3120
title: flag 求值错误语义的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3119]
created: 2026-09-17
---

## Question

FlagEvaluator 合同（五态/永不抛/兜底/分布/畸形）怎么钉住？（spec 2009 / effort #2009 / R10）

## Resolution

**七用例全绿**（首跑 1 红根因：成对校验只在工厂方法——canonical
构造 new FlagDefinition("v", null, "targeted") 绕过守约；约束移入
compact constructor 后 7/7）：静态 STATIC / 命中 TARGETING_MATCH 未
命中回默认 / 未注册 FLAG_NOT_FOUND+null / 谓词炸 DEFAULT 兜底且 ERROR
同记 / null ctx 正常 / 分布计数精确+注册面 / 畸形六型 fail-fast。
