# Spec 342 — 维护窗口 cordon（effort #342）

> wayfinder map：`.wayfinder/maps/effort-342.md`（T675–T676）。C 会话第 43 轮。

## Problem Statement

计划内维护（升级/迁移）期间需要「不接新会话、在途自然排空」——
现状没有承载：SpawnGate 地板（335）单写者，若维护逻辑直接抬地板，
维护结束落回时会压掉仍在生效的预算冻结；且没有时间窗与按钮两种
入口。

## Solution

- **`SpawnAdmissionFloor` 多源合成**：named source 各自 set(source,
  priority)；`get()` = 各源语义最高（序数最小），无源默认 LOW；
  `set(priority)` 保留为 default 源（335 ErrorBudgetPolicy 调用零改动）；
  `view()` 观测各源。维护与冻结正交——同向抬 HIGH，谁先结束谁落回，
  另一方仍生效。
- **`MaintenanceCordon`**（core.backpressure，SmartLifecycle 自管虚拟
  线程轮询）：
  - yml `buzhou.maintenance.{from, until, reason}`（ISO 时刻；from <
    until 校验；**过期窗启动即 no-op 不追溯**）——窗内抬 maintenance
    源 HIGH（进入时 WARN + `buzhou.maintenance.cordoned` 计数一次），
    窗出落回。
  - 运行时事故按钮 `cordon(reason)` / `uncordon()`（即时生效不重启
    ——325「按钮必须预先在场」纪律，bean 恒在无 yml 也装配）。
  - `view()`：{cordoned, reason, until}。
- 拒绝路径沿用 SpawnGate admission-floor 判定（低于地板即拒——
  342 不动 gate）。

## User Stories

1. 作为运维，我想声明维护窗自动 cordon/解除，所以 升级窗口新会话
   不进来、结束自动恢复。
2. 作为运维，我想运行时一键 cordon/uncordon，所以 事故现场临时
   停进不重启。
3. 作为 SRE，我想维护 cordon 与预算冻结互不覆盖，所以 维护结束
   后烧穿冻结仍然生效。
4. 作为审计者，我想 cordon 有计数与 WARN，所以 「为什么拒绝新
   会话」可事后回答。
5. 作为使用者，我不想配维护窗时行为与现状完全一致，所以 升级
   零风险。

## Implementation Decisions

- 地板合成在 get()（无锁读 ConcurrentHashMap.values() 取序数最小）；
  各源写互不干扰。
- cordon 轮询搭 335 同款虚拟线程（fixed-period interruptible sleep）；
  时钟可注入（测试伪时钟）。

## Testing Decisions

- 地板：多源 max/单源落回/default 源兼容/view。
- cordon：伪时钟入窗抬 HIGH 出窗落回 / 进窗 WARN 计数一次 /
  runtime 按钮即时生效 / 过期窗 no-op / 与冻结并存（budget HIGH +
  maintenance 落回 → floor 仍 HIGH）。
- 装配：bean 恒在；yml 窗绑定；from ≥ until 红。

## Out of Scope

- 多窗列表；在途会话 drain（318 PDB 职责）；cordon 时长上限护栏。

## Further Notes

- 新公共类型 `MaintenanceCordon` 随轮 regenerate 快照；地板方法级
  增量不增型。
