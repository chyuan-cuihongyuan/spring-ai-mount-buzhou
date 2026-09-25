# impl 2215 — T 会话 T15 Simple8b 位打包（spec 6014 / T6229–T6230 / T15）

纵切片：Simple8b（core/message）——16 档选择子贪心打包 +
尾零截断 + 混合量级 oracle。

- 验证：`mvn -pl buzhou-core test -Dtest='Simple8bTest'` 全绿（MVN_EXIT=0）。
