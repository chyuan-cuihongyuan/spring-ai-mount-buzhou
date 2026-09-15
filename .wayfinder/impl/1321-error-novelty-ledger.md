# impl 1321 — ErrorNoveltyLedger 错误首见签名台账（R22 = effort #1721 / spec 1721 / T2643-T2644）

**What**：record(signature) 返回是否首见+有界 256 FIFO 逐出（逐出后再现再「首见」诚实入档）+NoveltyReport/noveltyRatio（无样本 −1）。
**Why**：Sentry new-issue 追踪——回归探测第一信号（新签名=新病）。
**Verify**：ErrorNoveltyLedgerTest 3 断言全绿。 **Status**：done（2026-09-15）
