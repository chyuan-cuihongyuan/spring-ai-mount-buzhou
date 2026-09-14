# 1069 — 危险工具守卫判定读面

> 来源：J 会话第 69 轮 = effort #1069（[T1593](../../.wayfinder/tickets/T1593-dangerous-tool-stats-shape.md) / [T1594](../../.wayfinder/tickets/T1594-dangerous-tool-stats-verify.md) / impl 821）。借鉴：HITL 授权面对账（等待人工确认量是审批吞吐规划的直接输入）。guard HITL 主 hook 首轴。

## Problem Statement

`DangerousToolGuardHook.beforeTool()`（spec 07 HITL 主路径）的六条路径——禁用、未匹配、授权命中（once 消费/session 复用）、豁免放行、升级确认——全部零计数：**HITL 面积不可见**。宿主无法回答「多少调用在等待人工确认、豁免放行比例多高（豁免清单是否过宽）、授权缓存命中率如何」；审批积压与豁免滥用均无量化信号。

## 目标

- `DangerousToolGuardHook` 增量（guard，静态面）：六 `AtomicLong`。
  - `invocations`：beforeTool 入口计数（总桶）；
  - `disabledSkips`（机制关闭）/ `unmatchedSkips`（非危险工具）/ `authorizedSkips`（授权命中：once 原子消费与 session 复用）/ `exemptedSkips`（豁免放行）/ `escalations`（升级确认——等待人工）五个结局桶。
- 嵌套 `record DangerousToolStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**invocations = 五结局桶之和**（每入口恰落一桶）。

## 兼容性

纯增量读面：beforeTool 返回语义、once 原子消费、豁免审计事件逐位不变；静态面理由同 R46–R68 先例；无新配置项。

## Out of Scope

- 按工具名分桶（危险清单即配置面）。
- fingerprint 分布（参数敏感面——红线纪律）。
