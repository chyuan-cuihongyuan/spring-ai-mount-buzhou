# Spec 212 — 期望默认组合（effort #150）

> wayfinder map：`.wayfinder/maps/effort-150.md`（T576–T577）。

## Solution

`DatasetExpectations.ofCommonDefaults()`：四内置期望全开（非空输入/期望在场/
输入唯一/规模窗 [1,1000]）——多数管线的一句话门禁；规模窗不合身时自组
`of(...)`（窗口是部署口径，不猜）。
