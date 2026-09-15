# Spec 1715 — 工具开关使用台账（effort #1715，R16）（effort #1715，R16）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2631–T2632，impl 1315，impl Unleash / LaunchDarkly 特性开关审计）。借鉴：ToolKillSwitchHook 杀工具无痕：谁在何时以何因杀了什么、杀了多久——事故追溯与开关卫生（忘了放回去的开关）双缺位。

## Problem Statement

`KillSwitchUsageLedger`（core/exec，实例面 synchronized 有界台账）：recordKill(tool, reason, at)+recordRestore(tool, at) 配对累计封禁时长+entries()（旧→新，默认容量 128 满则逐出最旧 ring 纪律）+counters()[杀,放]+killedDurationMillis（已配对部分）。

## Solution

作为事故追溯者，台账给出 incident-42 期间 http 被杀 3 分钟的完整链。

## User Stories

1. 17150
2. 17151
3. 17152

## Implementation Decisions

- 17153

## Testing Decisions

- 17154

## Out of Scope

- 17155

## Further Notes

- 17156
