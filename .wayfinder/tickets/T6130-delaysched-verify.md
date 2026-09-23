---
id: T6130
title: S 会话 S15 延迟调度的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6129]
created: 2026-09-24
---

## Question

S15 合同怎么逐一验绿？（spec 5014 / effort #5014 / S15）

## Resolution

**验证通过**：DelaySchedulingTest 五测全绿——首选可用零跳过；
WAIT 累计到预算 ANY 启动并重置；预算=0 立即 ANY；负预算/null
fail-fast；确定性回放。
