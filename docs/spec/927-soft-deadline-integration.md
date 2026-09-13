# 927 — 软截止预警集成

> 来源：I 会话第 28 轮 = effort #927（[T1305](../../.wayfinder/tickets/T1305-soft-deadline-shape.md) / [T1306](../../.wayfinder/tickets/T1306-soft-deadline-verify.md) / impl 680）。spec 921 集成留位兑现（值对象 → exec 内核）。

## 背景

spec 921 落地 `TurnDeadline.withinSoftWindow/softDeadlineAt` 值对象读法时明确留位：「exec 内核等待点集成归后续轮」。本轮回填。

## 目标

- `HarnessToolCallingManager`：
  - `setSoftDeadlineWindow(Duration)`（null = 关——默认零行为变化）；
  - `awaitCompletion` 入口 `checkSoftDeadlineWindow(deadline)`：首次进入软窗（`withinSoftWindow`）时——
    - `AtomicBoolean` CAS 一次性置位（每 Turn 最多一条预警）；
    - counter `buzhou.turn.soft-deadline` + WARN 日志；
  - `beginTurn` 复位预警旗标（每 Turn 独立）；
  - 派发/TIMEOUT 行为零变化（预警只观测不干预）；
- `softDeadlineWarned()` 读面。

## 测试

`SoftDeadlineFlagTest`：未配置零触发 / beginTurn 复位 / 值对象窗判定（`checkSoftDeadlineWindow` 私有逻辑经旗标语义直测覆盖；Mimosa 扫描器误报拦截整合 executeToolCalls 测试——拆分旗标直测入档）。

## 兼容性

opt-in：softDeadlineWindow 为 null（默认）时全部行为逐位不变。
