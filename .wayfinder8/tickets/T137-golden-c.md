---
Type: task
Status: closed
blocked-by: T132, T133, T134
---
## Question

黄金轨迹 C：effort #7 新机制（半开多探测/目录溢出+检索/死信重放/保留清扫）的轨迹入集。

## Resolution

AFK 自决：GoldenTrajectoryEffort7Test 四轨迹：①阈值 2 半开（state-changed 序列 HALF_OPEN 出现且 CLOSED 在两次成功后）；②目录溢出提示 + skill_search 命中未列出技能；③死信→replayDeadLetters→补投递；④CLOSED 行过期 purgeOlderThan 淘汰。产 spec 38 §C + impl-110。
