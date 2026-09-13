# impl 558 — RouteDistributionReadout（effort #805）

## 切片

- `buzhou-resilience/src/main/java/.../resilience/routing/RouteDistributionReadout.java` — Collector（ConcurrentHashMap<String,AtomicLong> 封顶 32+truncated）；analyze/ gini 纯静态；Deviation/Report record。
- `buzhou-resilience/src/test/java/.../resilience/routing/RouteDistributionReadoutTest.java` — 6 例。

## 口径

- 排序：|deviation| 降序 + thenComparing(route) 破平（首测抓获 Map.of 无序+两路由偏差互反的天然打平——典序保证确定性）。
- gini 离散式 (2·Σi·x_i)/(n·Σx) − (n+1)/n；x=0 也参与 n（含零流量路由）。

## 验证

mvn -pl buzhou-resilience -am test -Dtest='RouteDistributionReadoutTest' → 6/6 绿；快照再生 1 新公共类型。
