# 745 — 响应缓存权重预算 yml 装配

> 来源：G 会话第 46 轮 = effort #745（737 权重预算的装配兑现，723 同模式）/ [T1092](../../.wayfinder/tickets/T1092-response-cache-weight-assembly.md) / [T1093](../../.wayfinder/tickets/T1093-response-cache-weight-assembly-verify.md) / impl 646。

## Problem

737 权重预算是构造原语——yml 无法启用（ResilienceModule 创建 ResponseCacheStore 只传 maxEntries/ttl）。「声明生效确认」缺失：配了也不知道生效与否。

## Solution

- `ResponseCache` record 扩第 4 组件 `maxWeightChars`（Long，默认 0=关；负值 fail-fast；@ConstructorBinding 规范构造+3 参兼容构造——R39/R48 多构造器坑规避）。
- ResilienceModule 装配点透传 `maxWeightChars()`+systemUTC 时钟。
- yml `buzhou.resilience.response-cache.max-weight-chars` + metadata 登记（ConfigDoctor 已知键宇宙同步）。

## Out of Scope

语义缓存之外的其它缓存面（只有两缓存，均已覆盖）。
