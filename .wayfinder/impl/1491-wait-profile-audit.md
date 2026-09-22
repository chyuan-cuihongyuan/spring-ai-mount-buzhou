# impl 1491 — WaitProfileAudit 阻塞期审计（R91 = effort #1890 / spec 1890 / T2981-T2982）

**What**：`WaitProfileAudit`（core/metrics 静态纯函数 + 嵌套
Profile/DominantClass）——profile（账面守恒 onCpu+lock+io=elapsed）
+ dominantClass（CPU>I/O>锁固定序取大）+ blockingRatio（阻塞占比）；
守恒破坏/负时长 fail-fast，零时长哨兵 0.0。

**Why**：Oracle ASH 会话状态语义——总时长一把尺没有分诊能力；
「算得慢加算力、等得久查锁换盘」的第一分诊面。与 StealTimeReadout
互补（外部偷 tick vs 内部时间构成）。

**Verify**：`WaitProfileAuditTest` 5 用例全绿（三类主导/并列固定序/
零时长哨兵/守恒破坏三例 fail-fast）。

**Status**：done（2026-09-23）
