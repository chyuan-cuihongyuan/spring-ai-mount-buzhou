# impl 2029 — Q 会话 R29 CLOCK 二次机会驱逐（spec 3028 / T5057–T5058 / R29）

纵切片：ClockEviction（core/cache）——环形帧+引用位+时针扫描+
逐出对账读数。

- 验证：`mvn -pl buzhou-core test -Dtest='ClockEvictionTest'` 全绿。
