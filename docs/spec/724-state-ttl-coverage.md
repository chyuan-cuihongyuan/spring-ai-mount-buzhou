# 724 — 会话状态 TTL 覆盖审计

> 来源：G 会话第 25 轮 = effort #724（状态存储治理）/ [T1048](../../.wayfinder/tickets/T1048-state-ttl-coverage.md) / [T1049](../../.wayfinder/tickets/T1049-state-ttl-coverage-verify.md) / impl 624。

## Problem

SessionStateStore 的每键 `ttlTurns`（Integer，null=永生）决定状态键是否随轮次过期——但「当前会话的状态键里有多少永生键、来自哪个 producer」没有读数面。永生键持续堆积是状态存储（memory/redis/jdbc 三实现）膨胀的主通道；542 只修了快照单点，覆盖面无从知晓。

## Solution

S3/MinIO 生命周期审计思想（无过期策略的对象=泄漏候选）：

- `StateTtlCoverage`（core/cleanup，纯函数静态原语）：
  - `analyze(Map<String, StateEntry>)` → `Report(totalKeys, persistentKeys, ttlKeys, coverage, byProducer)`；
  - persistentKeys = ttlTurns==null 的键数（永生键——膨胀候选）；
  - coverage = ttlKeys/totalKeys（total=0 → 1.0 空真——空集无键可泄漏，消费者结合 total 判断）；
  - byProducer：`Row(producer, keys, persistent, ttlKeys)` 按 producer 字典序——**哪个机制在写永生键**是治理焦点。

## User Stories

1. 存储治理：某会话 state 覆盖率 0.3、producer=x 的永生键 40 个——给该 producer 的键补 TTL 有证据。
2. 回归防护：新 hook/advisor 落地后覆盖率下跌=引入永生键——一眼可见。

## Implementation Decisions

- 纯函数（调用方供 getAll/scanByPrefix 投影——store SPI 零侵入）。
- producer=null 归一 "unknown"。

## Testing Decisions

- 双 producer 混合 TTL/永生：coverage 精确、producer 行字典序、persistent 计数。
- 空 map：total=0、coverage=1.0、rows 空；null fail-fast。

## Out of Scope

- 补 TTL 动作（宿主治理）。
- 轮次过期判定（store 读路径既有）。

## Further Notes

与 705/707/711 同主线：fsck 族治理面。
