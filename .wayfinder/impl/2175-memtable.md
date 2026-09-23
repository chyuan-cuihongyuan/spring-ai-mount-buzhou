# impl 2175 — S 会话 S25 MemTable 内存表（spec 5024 / T6149–T6150 / S25）

纵切片：MemTable（core/metrics）——有序 upsert + 满拒 + drain
交接。

- 验证：`mvn -pl buzhou-core test -Dtest='MemTableTest'` 全绿。
