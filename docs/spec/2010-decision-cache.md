# Spec 2010 — 判定决策缓存（effort #2010，R11）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3121–T3122，impl 1561）。
> 借鉴：OPA/Cedar decision cache——同输入判定 TTL 内短路。

## Problem Statement

护栏判定链（危险工具/注入分类/PII）对重复输入重复判定：同一条工具调用
签名在策略未变时反复全链评估——判定成本线性放大；而判定依据变更时又
需要显式失效（不能等 TTL 自然过期）。

## Solution

`DecisionCache<K, V>`（buzhou-guard decision 新子包，synchronized 小
临界区）：

- `get(key, now)`：TTL 内命中（hits++）；过期惰性清除（expirations++
  ——不计 miss，过期非未见过）；未存过 misses++；
- `put(key, verdict, now)`：重判回填刷新时间戳；超容量 LRU 驱逐
  （accessOrder，evictions++）；
- `invalidate(key)`：显式失效（策略热更新后精准失效）；
- `stats()` 四计数 + size + hitRate()（缓存有效性证——命中率低即该
  降级直判）；时钟回拨宽进（now−ts < 0 < ttl 命中，文档显式）；
- 契约：ttl > 0、maxEntries ≥ 1、key/verdict 非 null fail-fast。

## User Stories

1. 作为护栏作者，重复签名判定 TTL 内短路——判定链成本不随重复输入
   线性放大。
2. 作为策略运维，热更新后 invalidate 精准失效——不等 TTL。
3. 作为容量观测者，hitRate 显形——缓存无效（命中率低）可据此摘除。

## Implementation Decisions

- 时间外注入（确定性可回放）；LRU 用 LinkedHashMap accessOrder。

## Testing Decisions

- TTL 界内命中（999ms）与恰过期（1000ms）边界；过期惰性清除不计
  miss；LRU 驱逐最久未用；显式失效后 = miss；覆盖回填刷新时间戳；
  命中率分布（2/3）与空缓存不除零；回拨宽进；畸形六型 fail-fast。

## Out of Scope

- 不接具体判定链（DangerousToolGuardHook 接线归后续轮）；
- 不做负缓存语义区分（DENY 缓存 TTL 与 ALLOW 同参数——分型 TTL 留白）。

## Further Notes

- 与 FlagEvaluator（实验变体）正交：本件缓存「判定」不缓存「配置」。
