# impl 2179 — S 会话 S29 Segment Log 分段日志（spec 5028 / T6157–T6158 / S29）

纵切片：SegmentLog（core/recovery）——段满滚动 + 最旧段淘汰 +
存活窗口读数。

- 验证：`mvn -pl buzhou-core test -Dtest='SegmentLogTest'` 全绿。
