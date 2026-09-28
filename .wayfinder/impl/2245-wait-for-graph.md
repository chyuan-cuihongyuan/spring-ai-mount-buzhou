# impl 2245 — T 会话 T45 WaitForGraph 等待图死锁检测（spec 6045 / T6289–T6290 / T45）

纵切片：WaitForGraph（core/concurrent）——增量加边即时
环检测 + 规范环 + 受难者裁决。

- 验证：`mvn -pl buzhou-core test -Dtest='WaitForGraphTest'` 五测全绿。
