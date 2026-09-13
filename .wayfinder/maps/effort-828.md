# effort #828 — 连接池获取计时 DataSource 装饰器

- 会话：H 会话 800 系第 29 轮 ｜ spec [828](../../../docs/spec/828-timed-datasource.md) ｜ 票 [T1157](../tickets/T1157-timed-datasource.md)/[T1158](../tickets/T1158-timed-datasource-verify.md) ｜ impl581
- 借鉴：HikariCP 池等待指标（brettwooldridge/HikariCP ≈20K star 扩散——700 系 leak 借鉴的姊妹轮：acquire 与 query 分离计量）

## 勘察（排重）

- StoreLatencyRing（810）：store 操作计时——连接获取层缺位。
- WriteFailurePolicy（jdbc）：写失败策略非延迟。
- grep -i `getConnection.*time|pool.*wait`：无命中。

## 决定

`TimedDataSource`（store-jdbc）：DataSource 装饰器——getConnection/getConnection(user,pass) nanoTime finally 计时进 StoreLatencyRing（操作名 getConnection / getConnection-user-pass——与 810 store 操作名空间分离）；其余 DataSource 方法纯委托（logWriter/loginTimeout/unwrap/isWrapperFor/parentLogger）；异常照抛耗时照记；null fail-fast。喂点=装配侧包装池 bean（Spring Boot 自动装配零侵入）。

## 测试

两种 getConnection 计数+透传/异常照记照抛（pool exhausted）/委托七法（logWriter 同一性+loginTimeout+parentLogger+unwrap/isWrapperFor——unwrap 泛型 ClassCastException 修正为 DataSource.class）/双参 fail-fast——4 例绿。

## 诚实边界

getConnection 耗时含池等待+建连+驱动开销（不可拆分——Hikari 内部细分归其自身指标）；包装需装配侧显式（不动 Spring 自动装配）；ring 复用 810（操作名前缀区分域）。
