---
id: T2358
title: R4 http_request per-host 并发上限的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2357
created: 2026-09-15
---

## Question

N 会话第 4 轮：如何验收？

## Resolution

PerHostConcurrencyGuardTest 五断言（闸单测四 + 工具集成一）：关=恒放行零开销 /
上限拒绝与释放再进 / 跨 host 独立 / 占满名额的 call() 返回 limit_conn 拒绝文案且
hostLimitRejects=1、守恒式 attempts=successes+totalRejects、释放后放行 / 无闸时
非法方法照旧 method 桶（零变化）。tools 模块全量绿（112 用例）后单轮 commit。
