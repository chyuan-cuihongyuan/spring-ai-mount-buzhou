# 1437 — 评估数据集质量审计

> 来源：L 会话第 38 轮 = effort #1438（票 T2177 / T2178 / impl 1090）。**换题记录**：同源轴迁移（S1 快照恢复演练/847 已占重复轴——本轴取数据集退化条目）。借鉴：Cleanlab 数据质量（近重复之外的另一轴：退化条目——空/超短条目让分数虚高或虚低）。

## Problem Statement

`EvalDatasetStore` 的条目质量无审计面：空 input、空 expected、超短 input（信息量不足）让评估分数虚高或虚低——「这个数据集还能信吗」只能逐条翻看。DatasetNearDuplicateStats（重复轴）、DatasetExpectations（期望格式轴）各管一段，**退化/信息量**轴缺位。

## 目标

- `DatasetQualityAudit`（core/eval，纯函数静态面，private 构造）：
  - `analyze(List<EvalItem>)` → `record QualityReport(totalItems, emptyInputs, emptyExpecteds, shortInputs, inputLengthP50, inputLengthP95)`；
  - 退化口径：空/空白 input、空/空白 expected、短 input（非空但 <`SHORT_INPUT_THRESHOLD`=8 字符）；
  - 长度 P50/P95（升序秩插值 R-7；空集 0）；
  - `degenerateRatio()` 派生（(空 input+空 expected)/total；同条目双退化两桶同计——比可 >1；空集 -1 哨兵）。
- 与 847（重复轴）/DatasetExpectations（期望格式轴）三者辨义。

## 兼容性

纯函数零 IO；只读不裁决（下架/修复条目归宿主）。

## Out of Scope

- 语义级质量（相关性问题——LlmJudge 域）。
- 期望答案质量评分（Judge 域）。
- 重复检测（847 已有）。
