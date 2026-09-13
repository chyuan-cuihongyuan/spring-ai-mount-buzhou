# 917 — 健康评分端点装配

> 来源：I 会话第 18 轮 = effort #917（[T1285](../../.wayfinder/tickets/T1285-score-assembly-shape.md) / [T1286](../../.wayfinder/tickets/T1286-score-assembly-verify.md) / impl 670）。spec 905 装配轮留位兑现。

## 背景

spec 905 落地 `BuzhouHealthScore` 编程读面时明确留位：「端点接线留装配轮（机制轮/装配轮分离纪律）」。本轮回填。

## 目标

- `BuzhouHealthEndpoint.buzhouSnapshot()` 返回 Map 增加 `"score"` 段：`BuzhouHealthScore.compute(contributors)` 投影（score / tier / upCount / downCount / unknownCount / downMechanisms）；
- compute 抛 RuntimeException 时降级 `score = Map.of("scoreError", message)`（与既有 `safeDetails` 同风格——单点故障不炸端点）；
- `"mechanisms"` 段原样共存（既有消费者零影响；既有测试 containsKey 断言兼容）。

## 测试

快照含 score 段且数值正确（UP 全 100）；mechanisms 段共存；既有 BuzhouObservabilityAutoConfigurationTest 零回归。

## 兼容性

纯增量键；既有消费者按键取值不受影响。
