# impl 2007 — Q 会话 R7 P² 流式分位数（spec 3006 / T5013–T5014 / R7）

纵切片：PSquareQuantile（core/metrics）——五标记增量估计 + 抛物线
主路径/线性退路 + 前 5 样本诚实口径 + p 开区间校验。

- 验证：`mvn -pl buzhou-core test -Dtest='PSquareQuantileTest'` 全绿。
