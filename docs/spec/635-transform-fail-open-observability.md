# 635 — 变换 fail-open 可观测

> 来源：F 会话第 36 轮 = effort #600（spec 169 fail-open 静默缺口的可见性补全）/ [T920](../../.wayfinder/tickets/T920-transform-failopen-shape.md) / [T921](../../.wayfinder/tickets/T921-transform-failopen-verify.md) / impl 488。

## 背景

变换 fail-open（169）静默——「变换从未生效」与「从未失败」不可区分；上游格式一变，提炼全停而无人知。

## 目标

failOpenCount() + 计数 + 首次 WARN。

## 非目标

- 不改 fail-open 语义（绝不丢数据契约不变）。

## 设计

三路失败（异常/null/空白）同计；空原文短路不算；WARN 原子去重一次。

## 测试

2 用例：三路计数与原文照返 / 短路与校验。

## 兼容性

wrap 签名不变；纯增量观测。
