# impl 2160 — S 会话 S10 Clock-Sweep 缓存驱逐（spec 5009 / T6119–T6120 / S10）

纵切片：ClockSweepCache（core/cache）——环形帧 + 使用计数 +
衰减驱逐 + 确定性时钟指针。

- 验证：`mvn -pl buzhou-core test -Dtest='ClockSweepCacheTest'` 全绿。
