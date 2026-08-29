# Spec 138 — 轮次心跳（effort #114）

> wayfinder map：`.wayfinder114/MAP.md`（T463–T464）。借鉴：Temporal
> Activity heartbeat（长活动周期性报活，静默即异常信号）。

## Problem Statement

在飞轮次的「活着但不动」卡死（下游 hang、模型无响应但连接未断）在既有
防护面上不可见：TurnDeadline 是绝对时限（未到不报）、RunawayHook 数的是
行为（不动不涨）、观测 span 未关闭。缺一个「最近进展」的事实表。

## Solution

`runaway/TurnHeartbeat`：注册制在飞表（register/clear 与轮次起止对齐，天然
有界 1024 封顶满则 fail-fast——在飞数超界是上层闸失守信号）；hook 在模型/
工具/事件点 `beat(sessionId, now)` 刷新（未注册 id no-op——诚实：不在飞不算
进展）；`stalled(candidates, quietThreshold, now)` 只检候选（调用方持在飞
名单，本表不猜生命周期），quiet > 阈值者按停滞时长降序返回（最长停滞优先），
每个检出计 `buzhou.turn.stalled-detected`。

## User Stories

1. 作为 SRE，我以 quiet 阈值巡检在飞名单，所以「活着但不动」的卡死轮次
   带停滞时长上墙，视线先落最卡处。
2. 作为宿主开发者，register/clear 对齐轮次起止即接入，所以表天然有界、
   无需自管清理。

## Testing Decisions

- 红队：beat 刷新 + 未注册 no-op + clear 幂等；停滞检测候选制 + quiet 降序 +
  未注册不检；阈值严格大于边界；1024 封顶 fail-fast 不留半态 + clear 腾位；
  参数 fail-fast 五面。

## Out of Scope

- Hook 自动接线；停滞自动处置；持久化/分布式心跳。

## Further Notes

- 三正交：TurnDeadline 管「多久必须完」、RunawayHook 管「做了多少」、
  TurnHeartbeat 管「多久没动静」。
