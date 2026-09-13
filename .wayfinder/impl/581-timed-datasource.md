# impl 581 — TimedDataSource（effort #828）

## 切片

- `buzhou-store-jdbc/src/main/java/.../store/jdbc/TimedDataSource.java` — DataSource 全方法实现（2 计时+9 委托）。
- `buzhou-store-jdbc/src/test/java/.../store/jdbc/TimedDataSourceTest.java` — 4 例（StubDataSource 可控桩）。

## 口径

- 操作名：getConnection / getConnection-user-pass（与 810 store 操作名无碰撞）。
- unwrap/isWrapperFor 直接透传（不声明自己是 wrapper——语义归 delegate）。

## 验证

mvn -pl buzhou-store-jdbc -am test -Dtest='TimedDataSourceTest' → 4/4 绿；快照再生 1 新公共类型。
