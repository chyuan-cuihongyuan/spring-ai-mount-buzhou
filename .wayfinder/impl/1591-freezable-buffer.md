# impl 1591 — 可冻结分段缓冲（spec 2040 / T3181–T3182 / R41）

纵切片：`FreezableBuffer`（buzhou-spill 主，模块首入 P 系）+
`FreezableBufferTest`（七用例）。自动封冻、整段 drain、追加序快照。

- 测试：`mvn -pl buzhou-spill test -Dtest=FreezableBufferTest` 7/7 绿。
