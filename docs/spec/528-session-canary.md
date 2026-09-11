# Spec 528 — 跨会话泄漏金丝雀检测（effort #528）

> wayfinder map：`.wayfinder/maps/effort-528.md`（T809–T810）。E 会话第 29 轮。

## Problem Statement

单实例多会话/多租户下，A 会话专属标记出现在 B 会话回复 = 跨会话污染
（共享缓存串话/历史泄漏）——注入检测（CanaryGuard）不覆盖此语义，
泄漏探针空白。honeytoken 思想：诱饵值泄漏即信号。

## Solution

`guard.leak.SessionCanaryRegistry`：

- `plant(sessionId)` → 专属令牌 `BUZHOU-LEAKCANARY-<8hex>`（确定性：
  同会话重取同令牌；salt 实例注入防离线推演）。
- `detect(observerSessionId, text)` → `List<LeakFrom(leakedFromSession,
  token)>`：扫描文本中属于**其他会话**的已种令牌（观察者自己的令牌
  不算泄漏——正常回显）；命中计数 `buzhou.guard.leak.detected`。
- 注册表 LRU 256 有界（tag 有界纪律）；salt 构造注入。

## User Stories

1. 作为多租户宿主，我想给每租户会话种专属金丝雀并扫描其他租户的输出，
   so 跨租户污染（共享缓存串话）有探测信号。
2. 作为安全运维，我想泄漏命中可计数可查询， so 隔离失效频率可量化。

## Implementation Decisions

- 令牌确定性生成（同会话同令牌——扫描无需注册表旁路存储原文）。
- 概率探针语义诚实：模型改写/截断不保（非隔离机制，是泄漏探测器）。

## Testing Decisions

- A/B 双会话：B 输出含 A 令牌 → leak 指向 A；A 自己文本含自己令牌 →
  不算；无令牌文本 → 空；LRU 逐出后旧令牌不再识别；salt 变令牌变。

## Out of Scope

- 自动隔离；模糊匹配；跨进程。

## Further Notes

- 新公共类型 `SessionCanaryRegistry`（嵌套 `LeakFrom`）随轮 regenerate
  快照 + api-surface.md 加行。
