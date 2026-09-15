# impl 1445 — ReconnectBackoffLadder 重连退避阶梯（R45 = effort #1844 / spec 1844 / T2889-T2890）

**What**：`ReconnectBackoffLadder`（buzhou-mcp 静态纯函数）——delayMillis
指数爬升封顶（溢出安全：触顶先判后乘）+ verdict RETRY/GIVE_UP（边界含）；
畸形四型 fail-fast。

**Why**：Resilience4j retry/libpq 重连惯例思想——固定短间隔打爆对端、
无限指数涨首连等天荒地老、永不放弃僵尸永生；三段式统一策略面。

**Verify**：`ReconnectBackoffLadderTest` 4 用例全绿。

**Status**：done（2026-09-16）
