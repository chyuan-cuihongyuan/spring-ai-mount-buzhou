---
id: T3209
title: n-gram 特征提取的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

文本滑窗特征怎么统一口径原语化？（spec 2054 / effort #2054 / R55）

## Resolution

**信息检索经典纯函数 `NgramExtractor`（core/metrics）**：
charNgrams/wordNgrams 双口径（定长滑窗保出现序+distinct 去重保首现
（指纹）/false 保留重复（频次））+短于 n 整段/词数不足原词诚实边界
+空白分词短语窗——散落内联（CanaryGuardHook 等）统一收敛件。
