# impl 1594 — 快速重传触发器（spec 2043 / T3187–T3188 / R44）

纵切片：`FastRetransmitTrigger`（buzhou-resilience circuit 主）+
`FastRetransmitTriggerTest`（七用例）。阈值恰触、切换重计、触发清零。

- 测试：`mvn -pl buzhou-resilience test -Dtest=FastRetransmitTriggerTest` 7/7 绿。
