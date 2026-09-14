---
id: T2408
title: R29 熔断慢调用维度的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2407
created: 2026-09-15
---

## Question

N 会话第 29 轮：如何验收？

## Resolution

CircuitSlowCallTest 五断言：未注入 10 次慢成功 CLOSED（零行为）；注入后
4 慢不跳（样本不足）→ 5/5 慢 OPEN（零失败前提钉死「慢独立跳闸」）；全快
CLOSED；无时长 recordSuccess 不计慢；rate=0 IAE。resilience 389 用例零回归。
