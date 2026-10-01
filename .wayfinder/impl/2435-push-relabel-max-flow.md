# impl 2435 — X 会话 33 PushRelabelMaxFlow 推重标最大流（spec 10032 / X10065–X10066 / X33）

纵切片：PushRelabelMaxFlow 推重标最大流——preflow 满推+FIFO discharge 推重标最大流（core/concurrent）。

- 验证：`mvn -pl buzhou-core test -Dtest='PushRelabelMaxFlowTest'` 全绿。
