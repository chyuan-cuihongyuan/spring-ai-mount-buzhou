# 1410 — 工具入参校验读数

> 来源：L 会话第 11 轮 = effort #1410（票 T2121 / T2122 / impl 1063）。**换题记录**：H R21「校验错误聚合」因 ToolArgsValidator 半撞让位——本轴（拒绝分桶读面）为其显式留白的下半。借鉴：Pydantic ValidationError（错误按类别结构化，校验失败分布可量化）。

## Problem Statement

`ToolArgsValidator.validate`（工具执行前 schema 校验，三调用点：入参×2+结果 schema×1）只返回错误文本：**校验通过率、拒绝集中在哪类错误**（缺必填/类型不符/enum 越界/数值越界/长度越界/入参非 JSON）全不可见。「模型总传错参数」是感知还是事实、该改工具描述还是收紧 schema，无数据可断。

## 目标

- `ToolArgsValidator` 静态读数面（validate 为静态入口，计数内置自动覆盖全部调用点；BuzhouMetricsHolder 静态先例）：
  - `validations`（有效校验次数——无可校验结构的 schema 不入账）/ `accepted` / 派生 `rejectedDerived = validations − accepted`（守恒式）；
  - 七错误桶（**标记单源**：check() 错误文本与分桶共用 MARK_* 常量，杜绝口径漂移）：unparseableArgs / missingRequired / typeMismatch / enumViolation / rangeViolation（min/max 合并）/ lengthViolation / otherViolation；
  - **桶非互斥**：一次校验可含多类错误，Σ桶 ≥ rejected（如实入档）；
  - `validationStats()` 嵌套 `record ValidationStats` + `resetValidationStatsForTest()` 归零口。

## 兼容性

纯增量读面：校验语义/permissive 放行/错误文本逐位不变（文本改为常量拼接，内容同串）；无可校验结构不入账（口径显式）。静态面理由同 FileSandbox/WriteFileStats 先例。

## Out of Scope

- 按工具名 tag 分桶（基数红线）。
- 调用点维度区分（入参 vs 结果 schema——同为校验轴，分面留后续）。
- 错误消息结构化返回（改返回类型即破坏性变更，另轮）。
