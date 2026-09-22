# impl 1499 — LruKEviction LRU-K 驱逐（R99 = effort #1898 / spec 1898 / T2997-T2998）

**What**：`LruKEviction`（core/cache 持态 keeper + forget 清理）——
record（每键 K 深环形历史）+ evictVictim（倒数第 K 次最早者先逐，
历史不足 K 视作 -∞ 新键先让路）；K≥1/时间单调 fail-fast。

**Why**：Postgres 缓冲池 LRU-K——顺序扫描的扫描污染（一次全表扫
把热数据冲出）由 K 深度新近度根治；K=1 平滑退化 LRU。与 CLOCK/2Q
互补（排序准则 vs 分区/低摩擦）。

**Verify**：`LruKEvictionTest` 4 用例全绿（扫描抗性/K=1 退化 LRU/
倒数第 K 次最早先逐/forget 与畸形两型 fail-fast）。

**Status**：done（2026-09-23）
