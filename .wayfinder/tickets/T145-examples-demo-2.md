---
Type: task
Status: closed
blocked-by: T132, T133, T136
---
## Question

examples 增补演示：skill_search 用例、死信重放运维演示、迁移演示（用户可读场景）。

## Resolution

AFK 自决：Effort7CapabilitiesDemoTest 三用例：①目录截断场景下模型经 skill_search 找到并 load 隐藏技能（ScriptedChatModel 两步工具调用脚本）；②恒 500 端点→死信→修好→replayDeadLetters 一键恢复；③H2→内存迁移后续聊。产 impl-118。
