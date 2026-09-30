# impl 2429 — X 会话 X27 Latin Hypercube 拉丁超立方采样（spec 10026 / X10053–X10054 / X27）

纵切片：LatinHypercube——层置换+层内一点边际全覆盖（McKay 思想）（core/metrics）。

- 验证：`mvn -pl buzhou-core test -Dtest='LatinHypercubeTest'` 全绿。
