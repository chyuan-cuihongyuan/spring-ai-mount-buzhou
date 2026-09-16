# impl 1557 — LWW 寄存器（spec 2006 / T3113–T3114 / R7）

纵切片：`LastWriteWinsRegister`（core/concurrent 主）+
`LastWriteWinsRegisterTest`（八用例）。(ts, writer) 字典序定胜负、
平局确定性仲裁、幂等、merge 收敛、conflict/superseded 双对账面。

- 测试：`mvn -pl buzhou-core test -Dtest=LastWriteWinsRegisterTest` 8/8 绿。
