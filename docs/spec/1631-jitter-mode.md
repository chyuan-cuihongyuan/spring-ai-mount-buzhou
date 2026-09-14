# 1631 · 退避抖动模式可配（AWS jitter 思想）

> 来源：N 会话 R32（effort #1631 / T2413–T2414 / impl 1184）。附：ObjectMapper
> 复用审计（68 处全为 static final 类级单例——无反模式，留痕）。

## Problem Statement

既有退避抖动是 ±j 对称（EQUAL 语义）：高 attempt（capped=max）时密集落在
max 附近——重试风暴的防同步效果弱于 AWS 推荐的 full jitter；且与前次退避
相关的序列在持续拥塞下形成拍频。

## Solution

`JitterMode` 枚举 + `withJitterMode` 链式注入（默认 EQUAL=既有语义零行为）：
- **FULL**：`random(0, capped)`——AWS 推荐默认，防同步最优；
- **DECORRELATED**：`random(base, min(capped, prev×3))`——与前次去相关
  （prev 为会话内上次退避，volatile 字段）；
- **EQUAL**：`capped × (1 − j + 2jU)`——既有 ±j 对称。
yml `buzhou.resilience.jitter-mode`（顶层 record 扩参 + 兼容构造；非法值
fail-fast 带修法）。

## Testing Decisions

- `JitterModeTest` 四断言（反射驱动 computeBackoff 多采样）：EQUAL 全部落
  [1, cap×1.2]；FULL 落 [1, cap] 且低区/高区均有落点（全随机特征）；
  DECORRELATED 落 [base, min(cap, prev×3)]；parse 未知值 IAE。
- 回归：resilience 全量 393 用例。

## Out of Scope

- Retry-After 路径的模式分派（供应商显式指示优先——不抖动语义保留）。
- MCP 建连退避的模式化（524 的指数重试独立小面）。
