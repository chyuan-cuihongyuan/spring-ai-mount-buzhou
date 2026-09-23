# Spec 1922 — 降级链配置校验（effort #1922，R123）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3045–T3046，impl 1523）。借鉴：
> Resilience4j/LiteLLM（万星级）降级链惯例——备模型链配置的静态
> 校验：主模型不得出现在备链、备链不得重复、不得为空——配置错误
> 启动期拦住而非首次降级时暴雷。

## Problem Statement

降级链配置错误（主模型混进备链、备链重复、空链）通常在首次降级
那一刻才暴露——生产故障期发现配置错误是最坏时机；静态校验缺
独立判定面。

## Solution

`FallbackChainValidator`（core/transaction，静态纯函数）：

- `validate(primary, fallbacks)`：四规则——primary 非空；fallbacks
  非空数组；fallbacks 内无重复；primary 不在 fallbacks 中——返回
  错误列表（空 = 合法）；
- `isSane(primary, fallbacks)`：错误列表为空的便捷布尔。

## User Stories

1. 作为装配作者，主模型混进备链 → 启动期报错列出规则——不再
   首次降级暴雷。
2. 作为配置评审者，isSane 一行判——合法性口径统一。
3. 作为多错误收集者，一次校验列出全部错误而非首个——修一次到位。

## Implementation Decisions

- 纯函数零状态；返回 List<String> 错误描述（空 = 合法）；大小写
  敏感精确匹配（模型名口径归调用方）。

## Testing Decisions

- 合法链一例（零错误）；主模型混入备链；备链重复；空数组；多
  错误并列收集；isSane 便捷判定。

## Out of Scope

- 不做降级执行（归备模型降级链）；不做运行时健康感知。

## Further Notes

- 与备模型降级链（#5）互补：那是执行面，这是配置静态校验面——
  配置绑定矩阵（#13）思想在链配置上的延伸。
