# impl 1593 — 刻度轮定时器（spec 2042 / T3185–T3186 / R43）

纵切片：`TickWheelTimer`（core/exec 主）+ `TickWheelTimerTest`（七用例）。
散槽、圈数、tick 推进、幂等、取消。

- 测试：`mvn -pl buzhou-core test -Dtest=TickWheelTimerTest` 7/7 绿。
- 教训入档：圈数公式 (delay−1)/W；槽写回必须无条件。
