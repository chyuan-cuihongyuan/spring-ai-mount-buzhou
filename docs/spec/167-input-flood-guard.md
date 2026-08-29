# Spec 167 — 同输入泛洪防护（effort #202）

> wayfinder map：`.wayfinder202/MAP.md`（T531–T532）。借鉴：API 网关
> idempotency-key 重复风暴防护——完全相同输入的窗口内重复计数超阈值即拦。

## Problem Statement

坏循环的常见形态：同一会话反复发送<b>完全相同</b>的输入（模型复读死循环、
客户端重试风暴、脚本重放）——每次都真实打一次模型，烧 token 烧预算，
且循环不会自己收敛。轮内工具复读有 memo（147）治，输入面的重复泛洪没人治。

## Solution

`InputFloodGuardHook`（guard/hook，order 220——工具面护栏之前）：

- **计数**：beforeTurn 按 (sessionId, SHA-256(strip(input))) 记窗口内重复次数
  （空白差不构成不同输入——与事件日志幂等键同口径）。
- **裁决**：窗口内第 N 次相同输入（N > 阈值，默认 5）→ `HookResult.block`
  （可读理由：疑似循环/重放，请变化输入或稍后再试）+ 计数
  `buzhou.input-flood.blocked`；不同输入 / 窗口滑出 → 正常放行。
- **窗口**：TTL 滚动（默认 60s）——重复停止即自动复位；per-session 表 LRU
  1024 封顶（内存纪律）。
- 阈值/窗口构造可配；不挂 hook 零变化。

## User Stories

1. 作为宿主，复读死循环在第 6 次相同输入时被拦——不再无限烧模型调用，
   用户收到可读理由而非静默黑洞。
2. 作为运维，blocked 计数即「循环/重放风暴」信号——客户端缺陷定位入口。
3. 作为用户，改写输入立即恢复（相似不拦——只拦完全相同）。

## Implementation Decisions

- 精确哈希匹配（模糊相似归语义面——不误伤改写重试，诚实边界）。
- Clock 注入（窗口断言确定性）。

## Testing Decisions

- 阈值内重复放行、超阈值 block 文案；窗口滑出复位再计；不同输入零影响；
  空白等价同键；LRU 有界；会话隔离。

## Out of Scope

- 模糊相似；跨会话联合泛洪；泛洪事件 webhook。

## Further Notes

- 防泛洪双面：工具复读 memo（147）+ 输入泛洪（本轮）。
