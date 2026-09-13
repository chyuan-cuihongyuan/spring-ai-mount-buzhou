# 926 — 事实衰减预报读法

> 来源：I 会话第 27 轮 = effort #926（[T1321](../../.wayfinder/tickets/T1321-decay-forecast-shape.md) / [T1322](../../.wayfinder/tickets/T1322-decay-forecast-verify.md) / impl 679）。prometheus predict_linear 同思路（对象换成 fact 生命周期）：「还有多久衰出」一读即知。

## 背景

`FactDecayPolicy`（spec 604，letta 置信度衰减借鉴）有 decayed/injectable 正向计算——「fact 还剩几轮被过滤」的逆向预报缺失。批量预报可提前刷新高价值事实（衰减预警→主动 reinforce）。

## 目标

- `FactDecayPolicy.turnsUntilFloor(double confidence)`：
  - `floor > 0`：`⌈halfLifeTurns × log2(confidence / floor)⌉`（confidence ≤ floor 返回 0——已衰出）；
  - `floor == 0`：`Long.MAX_VALUE`（永不过滤——injectable 恒真语义一致）；
  - 校验 confidence ∈ (0,1]（越界 fail-fast）；
- 纯函数零行为变化（与 decayed/injectable 互逆性测试固化：`decayed(conf, turnsUntilFloor(conf)) ≥ floor` 或 turns==0）。

## 兼容性

纯增量：record 新增方法，零既有行为变化。
