# Spec 143 — 会话隔离检疫（effort #100）

> wayfinder map：`.wayfinder/maps/effort-100.md`（T495–T496）。借鉴：Erlang/OTP
> supervisor「let it crash + 指数退避重启」——反复失败的会话隔离冷却，
> 越闹冷却越长。

## Problem Statement

单会话可能进入失败循环（毒上下文/坏工具链/输入触发系统性错误）：每轮都失败、
每轮都真实消耗模型调用与 Turn 预算，且失败历史还留在上下文里继续毒化下一轮。
进程级组件用熔断隔离坏依赖，会话级缺等价物。

## Solution

`SessionQuarantine`（core/session）+ `SessionQuarantineHook`：

- **状态机（per sessionId）**：连续失败计数 ≥ failureThreshold（默认 3）→
  隔离至 releaseAt；隔离期内 `admitOrThrow` 抛
  `BuzhouException(SESSION_QUARANTINED)`（message 含剩余冷却与连败数）；
  到时<b>自动解除</b>（下次准入放行）。
- **指数升级**：第 n 次跳闸冷却 = base×2^(n-1) 封顶 max（默认 30s 起 / 10min 封顶）
  ——毒会话反复闹，冷却逐级加长。
- **成功复位**：`recordTurnSuccess` 公共 API（宿主/装配在健康轮调用即清零连败；
  hook 面看不到「健康轮」全貌——不谎装自动复位，诚实边界）。
- **hook**：beforeTurn 准入（隔离中 → `HookResult.block` 可读理由——不抛异常，
  模型与用户拿到「检疫中剩 Xs」）；onModelError 计一次失败。
- 计数 `buzhou.quarantine.tripped`；`snapshot()` 观测面。

## User Stories

1. 作为宿主，毒会话在第 3 次连败后被隔离——不再每轮烧模型调用；退避自动到时
   放行试探（恢复即用、再闹更冷）。
2. 作为模型/用户，隔离期请求拿到可读理由（剩多少秒）而非静默失败。
3. 作为运维，snapshot 看 哪些会话在隔离/第几跳——定位毒输入来源。

## Implementation Decisions

- per-session 单锁状态（对齐 ToolCircuitBreaker 风格）；Clock 注入。
- ErrorCode 追加 SESSION_QUARANTINED（NON_RETRYABLE）。
- hook order 40（最早裁决——隔离面高于一切护栏）。

## Testing Decisions

- 状态机：阈值跳闸/隔离抛/到时解除/指数升级/成功复位。
- hook：隔离期 beforeTurn block 文案；onModelError 累计触发跳闸；解除后放行。

## Out of Scope

- 人工解禁 API；检疫事件外发；跨实例共享检疫（本地失败本地隔离）。

## Further Notes

- 稳定性拼图：工具熔断（131）/ 模型熔断（15）/ 会话检疫（本轮）三层各管一层。
