# impl 2170 — S 会话 S20 热点 Key 探测器（spec 5019 / T6139–T6140 / S20）

纵切片：HotKeyDetector（core/metrics）——mod 采样 + 阈值告警 +
热点清单。

- 验证：`mvn -pl buzhou-core test -Dtest='HotKeyDetectorTest'` 全绿。
