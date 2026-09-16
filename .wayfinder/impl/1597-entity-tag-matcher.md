# impl 1597 — ETag 条件请求匹配（spec 2046 / T3193–T3194 / R47）

纵切片：`EntityTagMatcher`（core/webhook 主）+ `EntityTagMatcherTest`
（七用例）。强弱比较、304/412 判定、通配与列表。

- 测试：`mvn -pl buzhou-core test -Dtest=EntityTagMatcherTest` 7/7 绿。
