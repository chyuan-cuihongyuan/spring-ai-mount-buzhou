# Spec 537 — 死信原因分类计数（effort #537）

> wayfinder map：`.wayfinder/maps/effort-537.md`（T827–T828）。E 会话第 37 轮。

## Problem Statement

死信只有总量计数——4xx 永久失败（接收端配置错）与重试耗尽（瞬时故障）
不可分，治理动作无从分流。

## Solution

forwarder markDead 增 `buzhou.webhook.dead-reason` 计数（tag reason
有界集：4xx | 重试耗尽）。

## User Stories

1. 作为运维，我想按原因分看死信增长， so 配置错（4xx）找集成方、瞬时
   故障（重试耗尽）扩容重试——治理动作分流。

## Implementation Decisions

- reason tag 有界（两个来源枚举值），tag 有界纪律。

## Testing Decisions

- 既有 forwarder 测试回归（死信路径计数不炸）。

## Out of Scope

- reason 持久化；自动重放。

## Further Notes

- 无新公共类型——快照零 diff 预期。
