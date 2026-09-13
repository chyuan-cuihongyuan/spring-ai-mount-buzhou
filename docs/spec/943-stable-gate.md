# 943 — k 次防抖门

> 来源：I 会话第 43 轮 = effort #943（[T1329](../../.wayfinder/tickets/T1329-stable-gate-shape.md) / [T1330](../../.wayfinder/tickets/T1330-stable-gate-verify.md) / impl 692）。flaky CI 防抖惯例（quarantine/retry-until-green 的从严反形：k 次全过才放行）。

## 背景

`EvalGate.enforce` 单次判定：flaky 数据集或抖动 judge 下误报率高。防抖语义「k 次全过才过」需宿主循环实现——门内建后 CI 配置一处收口。

## 目标

- `EvalGate.enforceStable(String datasetName, Evaluator evaluator, double threshold, int k)`：
  - 循环 k 次既有 enforce（落盘/事件/历史/指标全继承）；
  - k 次全 passed → 结果 passed=true；任一失败 → false；
  - 返回**最后一次** GateResult（runId/passRate 为末次值；全量判定经 history() 可查）；
  - 校验 k ∈ [1, HISTORY_CAPACITY]（防历史环溢出丢失前序判定）；
- 既有单次 enforce 语义零变化。

## 兼容性

纯增量：公共类新增方法，零既有行为变化。
