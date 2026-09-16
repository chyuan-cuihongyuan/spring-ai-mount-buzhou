# impl 1551 — P 会话 2000 系对账门（spec 2000 / T3101–T3102 / R1）

纵切片：`PSession2000LedgerAuditTest`（starter 测试，OSession1800 同款
公式族第三应用）。四断言：票对存在（T3101+2(N−2000) 成对）、impl 存在
（1551+(N−2000)）、README 含号、spec 自 2000 严格递增；范围扫 spec 目录
自扩展。自举：本切片自身即 spec 2000 的四件套首例（spec+README+票对+impl）。

- 测试：`mvn -pl buzhou-spring-boot-starter test -Dtest=PSession2000LedgerAuditTest` 全绿。
- 后续每轮 commit 前复跑（对账门常驻）；R6k 对账轮全量核账。
