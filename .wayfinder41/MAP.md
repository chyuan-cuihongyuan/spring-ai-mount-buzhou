# Wayfinder Map — Buzhou 评估回归门（effort #41，50 轮自迭代第 6 轮）

> effort #41，延续 #40（T311–T312 / impl-226）。主线：评估闭环缺「CI 收口」——
> 数据集与 runner 有了，但 CI 里「低于阈值即红」需要宿主自己拼逻辑；Promptfoo
> 的 `--eval-min-pass-rate` / LangSmith eval-as-gate 是成熟形态。

## Destination

`EvalGate.enforce(dataset, evaluator, threshold[, parallelism])` → `GateResult`
（passed + passRate/threshold + 三态计数 + 失败项预览截 10 条 + CI 单行 summary）；
error 计入分母（回归门从严：基础设施故障不是绿）；threshold clamp 0..1；执行语义
复用 EvalRunner 既有管线（落盘/事件/注册表不重复）。

## Notes

- 借鉴：Promptfoo eval CI gate（阈值 + 失败预览）/ LangSmith eval-as-gate。

## Decisions so far

- error 计入分母（与 spec 52 passRate 口径一致——从严侧）。
- 预览截 10 条（CI 日志可读性优先；明细在 run 记录里）。

## Not yet specified

- run 对比 diff（两 run 逐项状态变化）；数据集版本化；Ragas 系指标（faithfulness/
  relevancy）；G-Eval 自定义 rubric。

## Out of scope

- 沿用 #7–#40；exit-code 直接绑定（宿主 CI 语义）；A/B 门（pairwise 胜率门——
  需求证据后议）。

## Tickets

- [x] [T313 EvalGate 判定面 + GateResult](tickets/T313-eval-gate.md)（impl-227）
- [x] [T314 4 例红队（过/不过/error 从严/截断+clamp）+ 文档收口](tickets/T314-eval-gate-close.md)
