# Spec 536 — 流式回复秘密扫描（effort #536）

> wayfinder map：`.wayfinder/maps/effort-536.md`（T825–T826）。E 会话第 36 轮。

## Problem Statement

秘密三缝（400）+ 回复 PII 流（500）之外——模型回复出站流里的**秘密**
（复读上下文密钥/生成示例密钥）无缝。500 StreamTextFilter SPI 需要第
二消费者证明组合性。

## Solution

`guard.secret.SecretScanStreamHook`（order 76）：滑动窗口（默认 128）+
占位符不拆分 + flush 排空（与 500 PII 流同构——算法同构独立实现，
安全域互不依赖）；复用 SecretScanner（7 型枚举域）；命中计数复用
`buzhou.guard.secret.redactions` + SecretHitStats OUTPUT；GuardModule
`secrets.stream-redaction`（默认关）。

## User Stories

1. 作为安全宿主，我想模型回复复读上下文密钥时在离场前占位符化， so
   秘密不进订阅者与观测面（400 第四缝闭环）。

## Implementation Decisions

- 与 500 PII 流同构独立实现（安全域互不依赖；两 hook 经 hook 序天然
  组合——SPI 组合性证明）。

## Testing Decisions

- 跨 chunk AWS 密钥合并占位符化；flush 排空；干净恒等；yml 装配/缺席。

## Out of Scope

- 会话历史回溯；组合排序策略。

## Further Notes

- 新公共类型 `SecretScanStreamHook` 随轮 regenerate 快照 + api-surface.md
  加行。
