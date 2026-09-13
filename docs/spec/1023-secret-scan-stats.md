# 1023 — 密钥扫描计数读面

> 来源：J 会话第 24 轮 = effort #1023（[T1497](../../.wayfinder/tickets/T1497-secret-scan-stats-shape.md) / [T1498](../../.wayfinder/tickets/T1498-secret-scan-stats-verify.md) / impl 776）。借鉴：Gitleaks（findings 数是密钥泄漏防线的第一水位）。与 R15/R19/R20 同族：安全判定静默显形。

## Problem Statement

SecretScanner（spec 400，7 型凭据签名 + 熵过滤）被 SecretScanHook / SecretScanStreamHook / GuardModule 生产接线，但 scan/redact 全程零计数：防线覆盖（扫了多少文本）、命中水位（发现多少凭据）、实际脱敏（替换了多少）不可见——泄漏趋势与阈值调优（熵阈值收紧后命中骤降=误报曾偏高）无据。

## 目标

- `SecretScanner` 增量（buzhou-guard secret 包，实例级）：`scanCalls` / `findings` / `redactions` 三 AtomicLong。
  - scanCalls：scan 实际执行计（空文本早退不计；redact 内部 scan 同计——口径入档）；
  - findings：命中条数按条累加（熵过滤丢弃的不计）；
  - redactions：redact 实际发生替换时计；幂等早返（已含占位符）不计。
- 嵌套 record `SecretScanStats(long scanCalls, long findings, long redactions)` + `stats()` 快照。

## 兼容性

纯增量读面：scan/redact 返回值与匹配语义逐位不变；无新配置项。

## Out of Scope

- 按 SecretType 分桶（7 型固定枚举可做——留作后续轮避免本轮过宽）。
- 扫描耗时分布（计时属 hook timing 域）。
