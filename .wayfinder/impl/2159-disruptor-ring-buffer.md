# impl 2159 — S 会话 S9 Disruptor 环形缓冲（spec 5008 / T6117–T6118 / S9）

纵切片：DisruptorRingBuffer（core/backpressure）——预分配槽 +
claim/publish 两段序标 + 非阻塞消费。

- 验证：`mvn -pl buzhou-core test -Dtest='DisruptorRingBufferTest'` 全绿。
