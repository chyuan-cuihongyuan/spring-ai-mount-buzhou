# impl 1596 — 属性白名单过滤器（spec 2045 / T3191–T3192 / R46）

纵切片：`AttributeWhitelist`（buzhou-observability pipeline 主）+
`AttributeWhitelistTest`（七用例）。盘内留盘外计数、通配、显式全拒。

- 测试：`mvn -pl buzhou-observability test -Dtest=AttributeWhitelistTest` 7/7 绿。
- 教训入档：校验必须先于集合构建（TreeSet 构造吞 null 即 NPE 逃逸
  fail-fast 契约）。
