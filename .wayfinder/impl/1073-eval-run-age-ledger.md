# 1073 — 评估运行年龄台账

**What to build:** EvalRunAgeLedger 公共静态面（recordOpened/recordClosed 埋点+Snapshot：active/oldestActiveAge 哨兵/maxCompleted 水位/closed）+ EvalRunRegistry Registration 双点埋点（registrationId 序号）+ 五测。

**Blocked by:** 全仓 verify 运行——完成后编码。

**Status:** done

- [x] EvalRunAgeLedger（core/eval，OPENED_AT 并发表+水位单调）
- [x] EvalRunRegistry 埋点（REGISTRATION_SEQ 移外层类——初版置嵌套类内跨作用域编译错评审修正）
- [x] EvalRunAgeLedgerTest 五测
- [x] spec 1420 + README 行（既有类埋点+新公共类→快照再生）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='EvalRunAgeLedgerTest,EvalRunRegistryTest,EvalRunnerTest'` 12/12 绿。
