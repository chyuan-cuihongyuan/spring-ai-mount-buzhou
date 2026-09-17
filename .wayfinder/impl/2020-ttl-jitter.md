# impl 2020 — Q 会话 R20 TTL 确定性抖动（spec 3019 / T5039–T5040 / R20）

纵切片：TtlJitter（core/cache）——按键确定性抖动 + 带宽夹持 + 1ms
兜底 + 参数 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='TtlJitterTest'` 全绿。
