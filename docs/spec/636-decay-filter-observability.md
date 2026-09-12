# 636 — 衰减过滤可观测

> 来源：F 会话第 37 轮 = effort #600（spec 626 装配后的运行信号补全）/ [T922](../../.wayfinder/tickets/T922-decay-filtered-obs.md) / [T923](../../.wayfinder/tickets/T923-decay-filtered-obs-verify.md) / impl 489。

## 背景

衰减装配后「滤了多少」不可读——策略是否真的在起作用无信号。

## 目标

`filteredCount()`（读时累计）。

## 测试

半衰序列计数断言（5/5）。

## 兼容性

getter 纯增量。
