# impl 1410 — FanoutPacingPlan 扇出 pacing（R10 = effort #1809 / spec 1809 / T2819-T2820）

**What**：`FanoutPacingPlan`（core/exec 静态纯函数）——`plan(fanout, interval,
headStart)` 逐任务起发延迟：头部名额 0（TCP IW），尾部 (i−headStart+1)×interval；
Plan 带 totalSpanMillis/pacedRatio（零扇出 -1 哨兵）；负扇出/间隔<1/名额越界
fail-fast。

**Why**：TCP 拥塞控制 pacing + IW 思想——扇出全发是惊群（下游瞬时过载、
限流集体触发），全量匀速又让小扇出白付延迟；头部免节流 + 尾部 pacing 两全。

**Verify**：`FanoutPacingPlanTest` 5 用例全绿。

**Status**：done（2026-09-16）
