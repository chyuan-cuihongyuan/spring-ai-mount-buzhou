# 820 — 护栏豁免登记面

> 来源：H 会话第 21 轮 = effort #820 / [T1141](../../.wayfinder/tickets/T1141-guard-exemption-registry.md) / [T1142](../../.wayfinder/tickets/T1142-guard-exemption-registry-verify.md) / impl 573。
> 换题注记：原 R21 校验错误聚合半撞（ToolArgsValidator 已有全错误聚合串）——换入豁免登记题。
> 借鉴：ESLint suppressions 带过期（≈26K star）。

## Problem

护栏告警的误报处置靠口头/注释：「这条我知道了别报」没有登记处——下次还响、审计时说不清谁豁免了什么到什么时候。

## Solution

`GuardExemptionRegistry`（guard 根包）：

- **显式有时限**：grant(mechanism, subject, untilMillis, reason)——机制×主体键；同键覆盖=续期。
- **判定**：exempt(mechanism, subject, now)——未过期 true；过期惰性移除+expiredTotal 计数。
- **有界**：封顶 64 条（满拒新+truncated 如实）。
- **解耦**：各护栏 hook 是否征询豁免由其自身接线——默认行为零变化。

## 兼容性

纯新增；零 hook 变更。

## 诚实边界

登记≠放行（hook 接线是显式步）；进程内存有界；subject 语义调用方定义。
