# impl 2241 — T 会话 T41 AIMD Window 加性增/乘性减窗口（spec 6041 / T6281–T6282 / T41）

纵切片：AimdWindow（core/ratelimit）——成败二值 AIMD 锯齿
窗口（源码本轮入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='AimdWindowTest'` 全绿（MVN_EXIT=0）。
