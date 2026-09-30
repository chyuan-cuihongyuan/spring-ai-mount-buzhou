# impl 2406 — X 会话 X4 Binary Fuse Filter 二进制熔合过滤器（spec 10003 / X10007–X10008 / X4）

纵切片：BinaryFuseFilter——静态键集三段熔合窗口+逆剥离构造（core/concurrent）。

- 验证：`mvn -pl buzhou-core test -Dtest='BinaryFuseFilterTest'` 全绿。
