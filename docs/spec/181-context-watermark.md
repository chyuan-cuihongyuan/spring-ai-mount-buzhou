# Spec 181 — 上下文余量水位（effort #209）

> wayfinder map：`.wayfinder209/MAP.md`（T553–T554）。借鉴：水库低水位预警
> ——撞线前先有预警线，运维有反应窗口。

## Problem Statement

上下文窗口耗尽的处置是「压缩预算线触发」（事后救火）：模型调用失败或压缩
紧急折入都是被动响应。「还剩多少」这一实时水位没有统一读数——低水位期间
正是该限制长工具输出/提前折入的窗口，却无人知道已进入低水位。

## Solution

`ContextWatermarkHook`（core/hook）：

- **估算**：beforeModel 时对本轮注入指令做字符量估算（零依赖口径），
  对照宿主配置的窗口字符容量 → 利用率。
- **gauge**：`buzhou.context.utilization`（0-1，活读最近值）+ 字符量
  `buzhou.context.chars`。
- **低水位**：利用率 ≥ 低水位线（默认 0.8）发一次会话事件
  `context.low-watermark`（payload: utilization/remainingChars）——<b>翻转制</b>
  （跨线上线各一次，不刷屏）；跌破恢复即发恢复事实。
- 纯观测不裁决（限制/压缩动作归既有机制——分层诚实）。

## User Stories

1. 作为运维，面板 utilization 曲线逼近 0.8 时就该关注——预警早于故障。
2. 作为策略，低水位事件触发保守策略（限制长输出/提前折入）——有钩子可挂。
3. 作为宿主，容量不配（0）= hook 静默零行为（零配置安全）。

## Implementation Decisions

- ModelCallContext.request() 取注入指令求和字符；容量/低水位线构造可配。
- per-session 翻转状态 LRU 1024。

## Testing Decimals

- 容量未配静默；低水位事件内容与翻转（只发一次）；恢复翻转；gauge 读数更新；
  多会话隔离。

## Out of Scope

- token 精算；自动压缩；per-model 差异容量。

## Further Notes

- 窗口治理链：余量水位（本轮，预警）→ 预算压缩（既有，动作）→ spill（兜底）。
