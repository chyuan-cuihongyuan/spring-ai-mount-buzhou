# Spec 1844 — 重连退避阶梯（effort #1844，R45）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2889–T2890，impl 1445）。借鉴：
> Resilience4j retry / libpq 重连惯例——指数退避+封顶+最大尝试后放弃
> 三段式。

## Problem Statement

MCP server 断线重连的延迟策略散落各处魔法数：固定短间隔打爆对端、无限
指数涨让分区恢复后首连等天荒地老、永不放弃让僵尸重连永生——「退避抚
网络、封顶保时延、上限知放弃」没有统一策略面。

## Solution

`ReconnectBackoffLadder`（buzhou-mcp，静态纯函数）：

- `delayMillis(attempt, base, multiplier, cap)` = min(base ×
  multiplier^(attempt−1), cap)——**溢出安全**（增幅触顶即返 cap，不做
  天文乘法）；
- `verdict(attempt, maxAttempts)` → `RETRY / GIVE_UP`（边界含：第
  maxAttempts 次仍 RETRY）；
- 契约 fail-fast：attempt < 1、base < 1、multiplier < 1、cap < base。

## User Stories

1. 作为连接治理者，100ms×2 封顶 10s → 前几次快速试探、后面拉长但
   网络恢复后首连最多等 10s。
2. 作为值班者，GIVE_UP 后转人工/兜底清单——僵尸重连不再永生。
3. 作为框架宿主，阶梯参自声明，纯算延迟零执行。

## Implementation Decisions

- 纯算不执行（连接管理归宿主）；溢出安全是硬约束（触顶先判后乘）。

## Testing Decisions

- 指数爬升 100/200/400/800；封顶钳制+天文 attempt 溢出安全；放弃边界
  含；畸形四型 fail-fast。

## Out of Scope

- 不做抖动（JitterMode 族已有）；不执行重连。

## Further Notes

- 与 McpServerBreaker 正交：那是熔断状态机，这是重连节奏策略面。
