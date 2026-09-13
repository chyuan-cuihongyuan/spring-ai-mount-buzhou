# 1045 — fs 沙箱判定计数读面

> 来源：J 会话第 45 轮 = effort #1045（[T1545](../../.wayfinder/tickets/T1545-sandbox-verdict-stats-shape.md) / [T1546](../../.wayfinder/tickets/T1546-sandbox-verdict-stats-verify.md) / impl 795）。借鉴：chroot escape detection（逃逸尝试频次与趋势是沙箱健康与攻击面探测的第一信号）。与 R18 命令沙箱分轴（那轴是命令执行结果，本轴是路径判定）。

## Problem Statement

FileSandbox（spec 06：read_file/write_file/copy_file/str_replace/run_command 共用路径沙箱）的拒绝判定只有逐次 WARN 日志：**路径逃逸尝试的累计频次与趋势**不可见——偶发笔误与系统性探测行为无法区分；spec 13 §cross-11「拒绝可观测」止步于日志行。

## 目标

- `FileSandbox` 增量（core/fs，实例级）：`resolutions` / `violations` 两 AtomicLong。
  - resolutions：resolve/resolveForWrite 入口计数；
  - violations：`violation()` 单点计数（路径越出/为空/解析失败全部汇于此——单点口径不变）。
- 嵌套 record `SandboxVerdictStats(long resolutions, long violations)` + `stats()` 快照——守恒 **violations ≤ resolutions**。

## 兼容性

纯增量读面：解析/异常语义逐位不变；无新配置项。

## Out of Scope

- 按 raw 路径分桶（路径含敏感面——红线纪律）。
- 逃逸升级阻断（fail-fast 既有语义不变）。
