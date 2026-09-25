# impl 2216 — T 会话 T16 Bit Packing 固定位宽打包（spec 6016 / T6231–T6232 / T16）

纵切片：BitPacking（core/message）——无缝串接+跨字合并取值
+九档位宽 oracle（初版 63 位域 limit=1<<63 溢出负数误判，
拆分校验分支根治；零位宽空字数组越界加守卫）。

- 验证：`mvn -pl buzhou-core test -Dtest='BitPackingTest'` 全绿（MVN_EXIT=0）。
