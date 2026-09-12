# Spec 531 — 装配绑定审计修复（effort #531）

> wayfinder map：`.wayfinder/maps/effort-531.md`（T815–T816）。E 会话第 32 轮。

## Problem Statement

R31 装配实证：单 Map 组件 record 构造绑定在 `prefix.<组件名>` 子路径——
凡 yml 键声明在 prefix 根的装配（409 result-schemas/406 deprecated/
505 experiments/502 capability）全部绑空静默 no-op（hasBean 断言挡不住：
bean 在、内容空）。

## Solution

- 四处装配 bean 统一改 **Environment 根绑定直读**
  （Binder.get(env).bind(prefix, mapOf(...))——与条件判定同一读法）。
- 405 Object 叶子值兼容 String/Number（属性源原文是 String）。
- 回归断言升级：hasBean → 内容非空（409 schemasCount==1、505
  experimentCount==1、502 registry.size()==1）。

## User Stories

1. 作为宿主，我想 yml 声明的词表/实验/退役/能力面在装配后**真的生效**，
   so 「bean 在但内容空」的静默 no-op 不再存在。

## Implementation Decisions

- 根绑定直读替代 properties 注入（与条件判定同一读法——一处真相）。
- 公共 record 类型保留（兼容面），不再作为装配注入载体。

## Testing Decisions

- 每处修复配内容非空断言（正例真值+负例缺席）。

## Out of Scope

- 组件名在 yml 路径中的既有装配（422/426/524 正确）。

## Further Notes

- 无新顶层公共类型（schemasCount 为加法方法）——快照零 diff 预期。
