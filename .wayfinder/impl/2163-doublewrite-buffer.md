# impl 2163 — S 会话 S13 DoubleWrite 双写缓冲（spec 5012 / T6125–T6126 / S13）

纵切片：DoubleWriteBuffer（core/recovery）——共享暂存 + 自动
落盘守恒 + 恢复视图 + fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='DoubleWriteBufferTest'` 全绿。
