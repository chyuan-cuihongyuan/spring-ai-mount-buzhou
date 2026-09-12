# effort #718 — 评估通过率漂移基线

- 会话：G 会话 700 系第 19 轮 ｜ spec [718](../../../docs/spec/718-eval-drift-baseline.md) ｜ 票 [T1036](../tickets/T1036-eval-drift-baseline.md)/[T1037](../tickets/T1037-eval-drift-baseline-verify.md) ｜ impl618
- 借鉴：Evidently AI（evidentlyai/evidently ≈10K star）drift detection——指标分布突变告警

## 勘察（排重）

- EvalRunner 五件套（预算 520/重试 535/超时 609/记忆化 708/期望 198）+EvalRunDiff（run 对 run）——**跨 run 通过率基线漂移**无面：连跑三次 95% 突然 60%，是数据坏了还是模型坏了？没人告警。
- grep drift：eval 族零命中。

## 决定

opt-in `setDriftBaseline(window, warnShift)`：run 完成后取同数据集**最近 window 次** run（startedAt 序）的 passRate 均值为基线，|当前−基线| ≥ warnShift → WARN+`buzhou.eval.drift.alerts` 计数+`lastDriftDelta()` 读数；无历史样本跳过；默认关零行为。用既有 run 记录落盘（eval.run.* 扫描）——零新存储。

## 测试

漂移触发计数/窗口取样限制/无历史跳过/默认关零行为。

## 诚实边界

通过率是粗粒度分布（item 级分数漂移归 detail 解析族）；只告警不阻断（run 照常落盘——漂移是信号不是错误）。
