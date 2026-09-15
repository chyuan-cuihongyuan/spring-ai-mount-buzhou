# impl 1344 — HedgeStats 对冲请求节省读面（R45 = effort #1744 / spec 1744 / T2689-T2690）

**What**：四计数+对冲赢率（决胜分母）+节省时延
**Why**：tail-at-scale/Envoy hedging——赢率定阈值激进度
**Verify**：HedgeStatsTest 2 断言 全绿。 **Status**：done（2026-09-15）
