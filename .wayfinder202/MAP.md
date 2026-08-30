# Wayfinder Map — Buzhou 同输入泛洪防护（effort #202，B 会话第 25 轮）

> B 会话第 25 轮。观察：坏循环里模型/用户会对同一会话反复发<b>完全相同</b>的
> 输入（模型复读 / 脚本重放 / 客户端重试风暴）——每轮都真实打模型。
> 借鉴 API 网关的 idempotency-key 重复风暴防护。

## Destination

InputFloodGuardHook（guard/hook）：beforeTurn 按会话追踪「相同输入哈希的
窗口内重复次数」——超阈值 block（可读理由：疑似循环/重放，请变化输入）；
窗口滑出自动复位；不同输入零影响。per-session 窗口表 LRU 有界。

## Notes

- 号段：B=奇数 spec（本轮 167）；B 轮次固定 .wayfinder200+。
- 与 TurnMemo（复读工具结果）呼应：那是工具面去重，这是输入面防泛洪。
- 哈希口径：SHA-256(strip(input))——空白差不构成不同输入。

## Decisions so far

- 只拦「完全相同」输入（相似去重归语义面——不误伤改写重试）。

## Not yet specified

- 泛洪事件 webhook；per-agent 阈值差异化。

## Out of scope

- 沿用各轮；模糊相似判定；跨会话联合泛洪。

## Tickets

- [x] [T531 InputFloodGuardHook（窗口重复计数+阈值 block）](tickets/T531-flood-guard.md)（impl-297）
- [x] [T532 泛洪回归（阈值 block/窗口复位/异文零影响/有界）](tickets/T532-flood-guard-tests.md)（impl-297）
