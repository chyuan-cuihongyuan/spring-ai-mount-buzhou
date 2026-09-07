# Spec 197 — 轮内模型调用循环闸（effort #217）

> wayfinder map：`.wayfinder/maps/effort-217.md`（T569–T570）。runaway 管工具步数循环、
> 检疫管跨轮失败循环——<b>轮内模型调用次数</b>这个总闸缺位。

## Problem Statement

一轮内模型调用可能被多种机制放大：advisor 重试、REASK 环路、onModelError
兜底再调、宿主自定义 advisor 链——每种各有自己的预算，但「本轮总共调了多少次
模型」没有总闸：任一机制配置失控时，一轮能悄悄打几十次模型（费用风暴）。

## Solution

`ModelCallCapHook`（core/hook，order 20——最早裁决）：

- **计数**：beforeModel 按 (sessionId, turn) 计数（ctx.turn()）。
- **裁决**：超过上限（默认 32，构造可配）→ `HookResult.block`（可读理由：
  本轮模型调用已达上限 N 次——疑似调用环路，请检查重试/REASK 配置）+
  计数 `buzhou.model-cap.blocked`。
- **清零**：键含 turn——下一轮自然独立；afterTurn 主动清（早清早释放）。
- 内存表 LRU 1024。

## User Stories

1. 作为宿主，任何机制配置失控都有最后一道闸——一轮模型调用费用有硬顶。
2. 作为运维，blocked 计数即「环路发生」信号——配置审计入口。
3. 作为模型，被拦时收到可读理由（而非静默黑洞）。

## Implementation Decisions

- 只数模型调用（工具循环归 runaway 语义面）；上限 ≥1 校验。

## Testing Decimals

- 阈值内放行逐次计数；超限 block 文案含计数；同会话隔轮独立计数；afterTurn
  清零后同轮再计（异常路径）；null 安全；LRU 有界。

## Out of Scope

- 上限 yml 配置；循环事件外发；跨轮累计。

## Further Notes

- 循环治理三闸：工具步数（runaway）/ 跨轮失败（检疫 143）/ 轮内模型调用（本轮）。
