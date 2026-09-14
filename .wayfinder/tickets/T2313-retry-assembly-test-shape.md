---
id: T2313
title: 工具瞬断重试装配链测试（R13 装配面补账）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2273
created: 2026-09-15
---

## Question

M 会话第 35 轮：spec 1511 的 autoconfig 装配链（buzhouIdempotentToolRetryAdapter）缺装配测试如何补？

## Resolution

**用户常设授权 AFK（可推翻）**

EvalPruneAssemblyTest 先例双用例：enabled=true 声明即 Holder 生效（RetryPolicy 透传 + transientOnly 白名单档 + overrides 经 ConfigMaps 数字键归一绑 List）；缺省无 bean Holder null 零行为。**装配测试当场实证 R13 缺陷**：env.getProperty(key, Duration.class) 在字符串属性源下无转换器（BeanCreationException）——改 DurationStyle 宽松解析（1s/250ms 简写 + ISO 双格式，非法值 BuzhouConfigurationException fail-fast 带修法）。
