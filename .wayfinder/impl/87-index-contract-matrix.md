# impl-87 — SessionIndexStore 契约测试矩阵

**What to build:** 三实现共享契约套件——语义漂移 CI 可检。

**Blocked by:** None

**Status:** done

- [x] AbstractSessionIndexContractTest 五契约（upsert 收敛/过滤排序分页/tag 精确/delete 幂等/空索引）
- [x] 三实现接入：InMemory（core）/ H2（JDBC + 持久用例）/ Redis（Testcontainers + 持久用例）
- [x] core 294 / jdbc 72 / redis 47 全绿；spec 33 §A

## Done

commit：见 git log（impl-87）。
