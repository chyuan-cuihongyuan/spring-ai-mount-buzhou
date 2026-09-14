# 1407 — PII 检测器合成探针自查

> 来源：L 会话第 8 轮 = effort #1407（票 T2115 / T2116 / impl 1060）。**换题记录**：原题工具调用 single-flight 勘察发现 ToolCallCoalescer 已存在（core/exec）——全撞换入 R29 题。借鉴：spaCy NER 评测 / Presidio analyze 打分（已知正负例穿测→逐类召回+误报，回归显形）。

## Problem Statement

`PiiDetector`（五类型正则+校验位/Luhn 验证）的规则回归——CustomPiiRules 变更、正则调整——只能靠线上漏报后追溯：无例行自查面。「检测器还认识邮箱/手机号/卡号/IP 吗」没有一秒钟出答案的口径。

## 目标

- `PiiProbeSelfCheck`（guard/pii，纯函数静态面，private 构造）：
  - 内建确定性合成样本池：正例按类型分组（EMAIL×2 / CN_PHONE×2 / BANK_CARD×1（业界通用测试 PAN，Luhn 合法）/ IPV4×2）；负例 4 条普通文本；
  - `probe(PiiDetector)` → `record ProbeReport(List<TypeRecall> recalls, int falsePositives)`；`TypeRecall(type, hits, total)` + 派生 `recall()` / `overallRecall()`；
  - 负例误报哨兵：falsePositives 应恒 0——非零即检测器规则过宽回归；
  - 报告按 PiiType 典序；探针只读可重复（同 detector 两次结果逐位一致）。
- **身份证号不入池**（红线）：CN_RESIDENT_ID 带校验位验证，构造校验位合法的合成号有撞真实证件号的概率——该轴由仓内既有单元测试覆盖，探针报告不含该类型。

## 兼容性

纯函数零状态零 IO；不改检测器/hook/红线配置；样本全部合成（非真实个人信息）。

## Out of Scope

- 探针结果台账化/JSONL 落盘（PiiHitStatsJsonl 同型另轮）。
- 定期调度接线（宿主例行化）。
- CustomPiiRules 自定义类型的探针扩展（静态池只覆盖内建五类的子集）。
