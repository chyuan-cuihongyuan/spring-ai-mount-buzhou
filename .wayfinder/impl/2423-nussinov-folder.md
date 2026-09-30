# impl 2423 — X 会话 X21 Nussinov RNA 二级结构折叠（spec 10020 / X10041–X10042 / X21）

纵切片：NussinovFolder——区间 DP 最大碱基对+无假结回溯（Nussinov 思想）（core/concurrent）。

- 验证：`mvn -pl buzhou-core test -Dtest='NussinovFolderTest'` 全绿。
