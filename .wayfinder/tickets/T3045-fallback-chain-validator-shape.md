---
id: T3045
title: 降级链配置校验的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

降级链配置的静态校验规则怎么定？（spec 1922 / effort #1922 / R123）

## Resolution`

**Resilience4j/LiteLLM 惯例纯计算 `FallbackChainValidator`
（core/transaction）**：validate 四规则（主模型非空/备链非空/备链
无重复/主模型不在备链）返回错误列表全收集 + isSane 便捷布尔。
配置错误启动期拦住而非首次降级暴雷。落轮 grep 复核无占坑。
