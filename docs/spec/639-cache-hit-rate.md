# 639 — 缓存命中率便利 getter

> 来源：F 会话第 40 轮 = effort #600（spec 53/55 的观测便利面）/ [T928](../../.wayfinder/tickets/T928-cache-hitrate-shape.md) / [T929](../../.wayfinder/tickets/T929-cache-hitrate-verify.md) / impl 492。

## 背景

两缓存 store 只暴露 hit/miss 计数，命中率自算散落调用方。

## 目标

`hitRate()`（0..1；零请求 0.0——与 PromptPrefixCache 同语义）。

## 测试

2 用例 + 全模块零回归。

## 兼容性

getter 纯增量。
