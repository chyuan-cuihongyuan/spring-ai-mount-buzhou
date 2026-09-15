# impl 1465 — VisibilityTimeoutAccounting 可见性超时账（R65 = effort #1864 / spec 1864 / T2929-T2930）

**What**：`VisibilityTimeoutAccounting`（core/webhook 静态纯函数）——
shouldRedeliver（边界含上）+ shouldDeadLetter（含上，零容忍合法）+
census 三段普查（在飞/超时重投/死信候选+最老在飞龄）；畸形 fail-fast。

**Why**：AWS SQS visibility timeout 思想——「至少一次」语义缺时限账则
消息永挂或毒消息死循环；取走隐藏+超时回队+穷尽死信三段两全。

**Verify**：`VisibilityTimeoutAccountingTest` 4 用例全绿。

**Status**：done（2026-09-16）
