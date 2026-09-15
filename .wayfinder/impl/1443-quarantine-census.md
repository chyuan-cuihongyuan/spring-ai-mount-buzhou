# impl 1443 — QuarantineCensus 隔离区普查（R43 = effort #1842 / spec 1842 / T2885-T2886）

**What**：`QuarantineCensus`（buzhou-guard 静态纯函数）——Quarantined 契约
构造 + census（待审/已审/最老旧 -1 哨兵 + pendingRatio）；空白 id/负龄期
fail-fast。

**Why**：邮件隔离区/恶意样本沙箱思想——疑似即杀误报无申诉、只放真阳
逃逸；暂存待审的积压读数（最老旧+占比）让缓冲不变黑洞。

**Verify**：`QuarantineCensusTest` 4 用例全绿。

**Status**：done（2026-09-16）
