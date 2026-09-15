# Spec 1734 — 技能漏斗读面（effort #1734，R35）（effort #1734，R35）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2669–T2670，impl 1334，impl PostHog / Amplitude 漏斗分析）。借鉴：技能「搜索→加载→应用」三级转化无漏斗：搜索命中没人加载（排序/描述问题）vs 加载没人应用（内容问题）不可分辨。

## Problem Statement

`SkillFunnelStats`（skill，实例面线程安全）：recordSearch/recordLoad/recordApply 三计数+census（loadRate 搜索→加载、applyRate 加载→应用，分母 0 哨兵 −1）。与 SkillUsageStats（用量面）互补。纯读面 opt-in。

## Solution

作为技能治理者，loadRate 低 → 描述/排序问题优先修。

## User Stories

1. 17340
2. 17341
3. 17342

## Implementation Decisions

- 17343

## Testing Decisions

- 17344

## Out of Scope

- 17345

## Further Notes

- 17346
