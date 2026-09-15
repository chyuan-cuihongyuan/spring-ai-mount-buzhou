# impl 1308 — LeaseRenewalStats 租约续期抖动读面（R9 = effort #1708 / spec 1708 / T2617-T2618）

**What**：静态纯函数 analyze(intervals)→RenewalReport(mean/cv/maxSkew)；n<2 哨兵 −1。
**Why**：续租节奏抖动先于租约丢失显形（etcd keepalive 思想）。
**Verify**：LeaseRenewalStatsTest 4 断言全绿。 **Status**：done（2026-09-15）
