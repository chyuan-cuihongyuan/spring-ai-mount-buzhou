# Spec 35 — 韧性/技能目录/媒体摄取增补

> effort #7（T118–T120 / impl-93–95）。

## §A 熔断半开多探测（T118 / impl-93）

- `Circuit.halfOpenSuccessThreshold`（默认 1 = 单探测即恢复的既有行为零变化）：>1 时
  半开需**连续 N 次探测成功**才回 CLOSED——抖动 provider（成功率 ~50%）不再
  「单探测成功→CLOSED→立刻再跳」循环。
- **探测槽位不变量**：`在飞探测数 + 已成功数 ≥ 阈值` 即占位满员拒绝（每成功永久占一槽）；
  任一探测失败立即回 OPEN（trips 递增、退避生效）；逃生窗口（2×生效冷却）外重置槽位。
- 7/6 参便捷构造保留（源/二进制兼容）；配置键
  `buzhou.resilience.circuit.half-open-success-threshold`（fail-fast ≥1）。
