---
id: T1327
title: 数据集输入长度画像读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 42 轮：EvalDatasetStore 有 items/fingerprint——「数据集输入长度分布画像」（评估成本与超长项探测的依据）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 42 轮 = effort #942 / spec 942 / impl 691（票号改号：T1317/T1318 已被 spec 937 占用））：缺口成立——评估成本 ∝ input 字节总量，超长项（P95 尾部）是预算失控点。落点 `EvalDatasetStore.inputLengthProfile(datasetName)`：`record InputProfile(int count, long totalChars, double avgChars, int maxChars, int p95Chars)`——input 字符长度统计（count/total/avg/max/p95=P95 分位插值，909 同口径内联实现避免跨类依赖）；数据集未建 fail-fast；空集约定全 0。纯查询零行为变化。
