# 1613 · 工具目录漂移看门狗接线（spec 201 孤类救活）

> 来源：N 会话 R14（effort #1613 / T2377–T2378 / impl 1166）。spec 1611 普查修复
> 第三弹：CatalogDriftWatcher（spec 201）与 ToolCatalogFingerprint（spec 175）
> 建成即孤——skills 镜像类（SkillCatalogDriftWatcher）已接线，core 版漏接。

## Solution

- `CatalogDriftHolder`：进程级单例（RetryBudgetHolder 同款模式）——基线跨会话
  收敛；emitter = WARN 日志 + `buzhou.catalog.drifted` 计数；`setWatcher` 测试可替换。
- 接线点：`HarnessAssembler.assemble` 的最终工具目录组装处（autoTools + extraTools
  + 包装后）——**会话构造节拍**拍指纹（每 spawn 一次，成本 = N 个 ToolDefinition
  的指纹哈希）。
- 首拍建基线（装配期变化是常态不发事件）；包装层不改 ToolDefinition——指纹只
  捕捉目录语义变化（增/删/改），不误报包装叠层。

## Testing Decisions

- `CatalogDriftWiringTest`：三会话序列（[a,b] → [a,c] → [a,c]）——首会话建基线
  零事件、次会话漂移一事件（added=[c]、removed=[b] 明细断言）、第三会话稳定
  无事件；Holder 直喂面（snapshot + check diff）。

## Out of Scope

- 定时调度拍指纹（会话构造节拍已够——低频会话场景可后续加调度）。
- 其余 core 读数孤类（MetricFreshnessTracker/LeakSuspectAggregator 等——每项独立轮）。
