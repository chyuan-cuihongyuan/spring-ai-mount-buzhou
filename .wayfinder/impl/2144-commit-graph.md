# impl 2144 — R 会话 R44 提交图世代号（spec 4043 / T6087–T6088 / R44）

纵切片：CommitGraph（core/policy）——世代号 O(1) 读 + gen
剪枝快道 + 有界 BFS 精确祖先判定 + 拓扑序注册 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='CommitGraphTest'` 全绿。
