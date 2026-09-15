# impl 1469 — IntervalSchedule 区间合并与缝隙（R69 = effort #1868 / spec 1868 / T2937-T2938）

**What**：`IntervalSchedule`（core/exec 静态纯函数）——merge（重叠/相邻/
嵌套归一）+ gaps（窗口内三段缝+越界裁剪+早退）；倒置/null fail-fast。

**Why**：日历调度 busy/free 惯例思想——占用段散落重叠则总忙时要人肉拼、
档期要人眼找；合并+缝隙是排程前置基建。

**Verify**：`IntervalScheduleTest` 4 用例全绿。

**Status**：done（2026-09-16）
