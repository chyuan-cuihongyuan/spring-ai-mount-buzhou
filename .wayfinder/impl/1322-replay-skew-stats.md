# impl 1322 — ReplaySkewStats 回放时钟偏斜读面（R23 = effort #1722 / spec 1722 / T2645-T2646）

**What**：record(originalAt, replayedAt) 偏斜累积+负偏斜（时钟倒挂）分离计数+SkewReport(median/max/negativeCount，无正样本 −1)。
**Why**：Kafka consumer lag/NTP 偏斜——回放健康度与回放机时钟可信度。
**Verify**：ReplaySkewStatsTest 3 断言全绿。 **Status**：done（2026-09-15）
