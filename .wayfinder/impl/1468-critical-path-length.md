# impl 1468 — CriticalPathLength 关键路径（R68 = effort #1867 / spec 1867 / T2935-T2936）

**What**：`CriticalPathLength`（core/exec 静态纯函数）——longestPath
（Kahn 拓扑+EF DP）→ Result（criticalPathMillis+terminalTask）；环/端点
缺失/重复任务/畸形 fail-fast。

**Why**：项目管理 CPM 思想——并行 DAG 总时长=最长加权路径；缩非关键
白花力气、关键延一秒总长延一秒——「该优化哪个」是数学不是直觉。

**Verify**：`CriticalPathLengthTest` 5 用例全绿。

**Status**：done（2026-09-16）
