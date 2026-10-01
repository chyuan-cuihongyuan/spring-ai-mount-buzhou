# impl 2448 — X 会话 46 AdamOptimizer 自适应矩估计（spec 10045 / X10091–X10092 / X46）

纵切片：AdamOptimizer 自适应矩估计——一阶/二阶矩 EMA+bias 修正更新环（core/metrics）。

- 验证：`mvn -pl buzhou-core test -Dtest='AdamOptimizerTest'` 全绿。
