# 942 — 数据集输入长度画像读面

> 来源：I 会话第 42 轮 = effort #942（[T1327](../../.wayfinder/tickets/T1327-input-profile-shape.md) / [T1328](../../.wayfinder/tickets/T1328-input-profile-verify.md) / impl 689 续）。评估成本画像（input 字节 ∝ token 成本）——超长项与预算失控点探测。

## 背景

`EvalDatasetStore` 有 items/fingerprint/tags——「这批数据集输入多长、长尾在哪」无读面。评估成本 ∝ input 字节总量；P95 尾部项是预算失控点。

## 目标

- `EvalDatasetStore.inputLengthProfile(datasetName)`：
  - `record InputProfile(int count, long totalChars, double avgChars, int maxChars, int p95Chars)`；
  - p95 = 线性插值分位（spec 909 同口径内联）；空集约定全 0；数据集未建 fail-fast（EVAL_OPERATION_INVALID 同口径）；
- 纯查询零行为变化。

## 兼容性

纯增量：公共类新增方法 + 公共嵌套 record；零既有行为变化。
