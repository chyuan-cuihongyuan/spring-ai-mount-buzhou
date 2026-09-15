# impl 1404 — MemoryPromotionAudit 层代晋升审计（R4 = effort #1803 / spec 1803 / T2807-T2808）

**What**：`MemoryPromotionAudit`（buzhou-memory 静态纯函数）——CycleFacts 单
周期事实（构造器核非负+去向之和=产出契约，畸形 fail-fast）+ `analyze` →
PromotionReport（四总计、promotionRate/directArchiveRate 无产出 -1 哨兵、
prematurePromotionCycles 过早晋升计数）。

**Why**：JVM 分代 GC 晋升诊断思想——晋升率常高=微压缩（年轻代）没拦住短命
内容，摘要（老年代）被灌水；过早晋升（有产出零保留）周期堆积=年轻代缓冲
失效，容量错配的经典信号。

**Verify**：`MemoryPromotionAuditTest` 4 用例全绿。

**Status**：done（2026-09-16）
