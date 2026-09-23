# impl 1521 — HealthScoreComposite 复合健康分（R121 = effort #1920 / spec 1920 / T3041-T3042）

**What**：`HealthScoreComposite`（core/health 静态纯函数 + Band 枚举）
——composite（Σ(score×w)/Σw 加权合成 0–100）+ band 三档判级
（≥80 HEALTHY/≥50 DEGRADED/UNHEALTHY 含下）；同长非空/分数越界/
零权重 fail-fast。

**Why**：监控面板复合健康分惯例——多维读数各自为政，「总体健康吗」
要人肉权衡；加权合成单一分 + 三档判级让看板告警一句话可答，权重
即运维语义。与 BuzhouHealth 单机制面互补。

**Verify**：`HealthScoreCompositeTest` 4 用例全绿（等权 70/带权
52.5/判级含下三档/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
