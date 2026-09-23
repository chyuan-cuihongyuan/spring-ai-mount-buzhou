# impl 2161 — S 会话 S11 Hinted Handoff 暂代投递（spec 5010 / T6121–T6122 / S11）

纵切片：HintedHandoff（core/recovery）——路由分叉 + hint 记账
+ 恢复 FIFO 回放 + 状态机 fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='HintedHandoffTest'` 全绿。
