# impl 2193 — S 会话 S43 Xor Filter 异或过滤器（spec 5042 / T6185–T6186 / S43）

纵切片：XorFilter（core/metrics）——三散列三槽 + 剥洋葱构建 +
异或查询 + checksum 确定性审计。

- 验证：`mvn -pl buzhou-core test -Dtest='XorFilterTest'` 全绿（MVN_EXIT=0）。
