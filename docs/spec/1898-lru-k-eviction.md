# Spec 1898 — LRU-K 驱逐（effort #1898，R99）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2997–T2998，impl 1499）。借鉴：
> PostgreSQL（15K+ 星）缓冲池 LRU-K 语义——按「倒数第 K 次访问时间」
> 淘汰：一次全表扫描不再把热数据冲出缓存（扫描抗性），K=1 退化为
> 传统 LRU。

## Problem Statement

纯 LRU 对顺序扫描零抗性：一次全表扫描把真热键全部冲出（扫描污染）；
频率类（LFU）又要长期计数。按倒数第 K 次访问的新近度淘汰，让「只
被扫过一遍」的键先走——K 择一的驱逐语义没有独立判定面。

## Solution

`LruKEviction`（core/cache，持态小keeper + 嵌套 Victim）：

- `record(key, nowMillis)`：记录访问（环形历史保留每键倒数 K 次）；
- `evictVictim(nowMillis)`：在候选集中取「倒数第 K 次访问最早」者
  驱逐（历史不足 K 次者视作 -∞ 优先驱逐——新键未证热度先让路，
  Postgres 同款）；
- K 构造注入（K=1 退化为传统 LRU）。

## User Stories

1. 作为缓存作者，热键 A 访问 5 次、扫描键 B 一次：K=2 时 B 的倒数
   第 2 次 = -∞ → 先驱逐 B——热数据保住。
2. 作为退化验证者，K=1 序列与 LRU 一致——旧语义可平滑回退。
3. 作为容量评审者，同候选取历史最早——确定性可回放。

## Implementation Decisions

- 每键环形历史（数组，K 小定长）；候选为全键集（容量管理归调用
  方）；K ≥ 1、now 单调 fail-fast（时间倒退拒绝）。

## Testing Decisions

- 扫描抗性主例（A 热键/B 扫描键 K=2 先逐 B）；K=1 退化等价 LRU
  序；同候选取最早；畸形两型（K=0/时间倒退）fail-fast。

## Out of Scope

- 不做容量管理与真实存储（归缓存面）；不做 Clock/2Q 变体（各有
  占位）。

## Further Notes

- 与 CLOCK 二次机会（Q-3028 近似 LRU）互补：那是无历史低摩擦，
  这是 K 深度新近度；与 TwoQueueCache（双队列分区）互补：那是
  分区，这是排序准则。
