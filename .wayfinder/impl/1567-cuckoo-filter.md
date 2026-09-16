# impl 1567 — 布谷鸟过滤器（spec 2016 / T3133–T3134 / R17）

纵切片：`CuckooFilter`（core/session 主）+ `CuckooFilterTest`（八用例）。
指纹双桶、确定性踢出、删除撤销、溢出计数。

- 测试：`mvn -pl buzhou-core test -Dtest=CuckooFilterTest` 8/8 绿。
- 教训入档：Java 三目条件必须是 boolean——int 槽位判定写 >= 0。
