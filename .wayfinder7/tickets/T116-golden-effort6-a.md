---
Type: task
Status: closed
blocked-by: T115
---
## Question

黄金轨迹扩充 A（新能力全覆盖的第一批）：outbox（重启恢复/死信）、evidence 引用（保留/释放）、压缩（事件化后）。轨迹如何设计成确定性？

## Resolution

AFK 自决：examples golden 增 GoldenTrajectoryEffort6Test。①evidence：DiskSpillStore 直驱（fork 引用→源删保留→释放物理删）+ 断言 WAL 级别（store 行为，非 session 事件——EventSequenceAssert 不适用，轨迹断言用 assertThat 步骤序列注释化）；②outbox：HttpServer 500→close→新 forwarder 同 store→200 补投（复用 core 测试思路但作为 examples 黄金演示面）；③压缩：ScriptedChatModel 大工具结果 + spill 配置 → memory.compacted 观测事件断言。产出 spec 34 §B + impl-91。
