# impl 2109 — R 会话 R9 增量+基准帧编码（spec 4008 / T6017–T6018 / R9）

纵切片：DeltaFrameOfReference（core/message）——帧化增量 + 基准重置
+ zigzag varint 复用。

- 验证：`mvn -pl buzhou-core test -Dtest='DeltaFrameOfReferenceTest'` 全绿。
