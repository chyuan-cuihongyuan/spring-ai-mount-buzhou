# impl 2197 — S 会话 S47 TinyLFU Admission 准入策略（spec 5046 / T6193–T6194 / S47）

纵切片：TinyLfuAdmission<K>（core/cache）——4 位饱和 count-min
双行 sketch + 准入裁决 + 减半老化。

- 验证：`mvn -pl buzhou-core test -Dtest='TinyLfuAdmissionTest'` 全绿（MVN_EXIT=0）。
