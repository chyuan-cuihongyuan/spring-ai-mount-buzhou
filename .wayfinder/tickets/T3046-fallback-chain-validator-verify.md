---
id: T3046
title: 降级链配置校验的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3045]
created: 2026-09-23
---

## Question)

校验在合法/四规则/多错误收集下正确吗？（spec 1922 / effort #1922 / R123）

## Resolution`

**FallbackChainValidatorTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=FallbackChainValidatorTest）：合法链零错误；主模型混入/
备链重复/空数组各拦；多错误并列收集；isSane 便捷判定。
