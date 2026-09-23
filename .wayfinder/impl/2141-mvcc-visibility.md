# impl 2141 — R 会话 R41 MVCC 快照可见性（spec 4040 / T6081–T6082 / R41）

纵切片：MvccVisibility（core/transaction）——事务注册 + 快照
捕获 + 纯函数可见性判定 + 收尾 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='MvccVisibilityTest'` 全绿。
