# impl 1552 — HLL 基数素描（spec 2001 / T3103–T3104 / R2）

纵切片：`HllCardinalitySketch`（core/metrics 主）+ `HllCardinalitySketchTest`
（七用例）。确定性 FNV-1a 64 + splitmix64 散列（无随机可回放）、寄存器
rank-max 幂等、调和平均 + 小值域线性计数修正、merge 逐位 max 并集、
precision [4,16] fail-fast、relativeErrorBound=1.04/√m 读数、
offeredCount 重复率对账面。

- 测试：`mvn -pl buzhou-core test -Dtest=HllCardinalitySketchTest` 7/7 绿。
- 换静脉记录：R2 原排 HDR Histogram——O-1860 LogBucketHistogram 已标
  HdrHistogram 思想（占坑），换 HLL（R5 原位提前）。
