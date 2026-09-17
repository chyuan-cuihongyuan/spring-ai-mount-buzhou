# impl 2028 — Q 会话 R28 OR-Set 观察删除集（spec 3027 / T5055–T5056 / R28）

纵切片：ObservedRemoveSet（core/concurrent）——唯一标签 add +
观察墓碑 remove + 双并集 merge。

- 验证：`mvn -pl buzhou-core test -Dtest='ObservedRemoveSetTest'` 全绿。
