# impl 1446 — ArgvBudgetGate argv 预算门（R46 = effort #1845 / spec 1845 / T2891-T2892）

**What**：`ArgvBudgetGate`（buzhou-tools 静态纯函数）——totalBytes 字节账
（NUL 分隔）+ verify 双闸三态（单参先于总量、边界含上）+ 默认常量；
负预算/零上限/null 元素 fail-fast。

**Why**：Linux execve ARG_MAX/MAX_ARG_STRLEN 思想——参数超限是 E2BIG
直接拒跑不是性能问题；先验后拼把系统级拒绝前移到参数校验层，巨型
argv 注入面（参数炸弹）同时封顶。

**Verify**：`ArgvBudgetGateTest` 3 用例全绿。

**Status**：done（2026-09-16）
