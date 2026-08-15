---
Type: task
Status: closed
blocked-by: T113
---
## Question

黄金轨迹扩充 B：export/import 往返、工具结果限幅、索引生命周期（含 DELETED 联动）。哪些走事件断言、哪些走行为断言？

## Resolution

AFK 自决：①export/import：行为断言（JSON 往返保真 + 重映射续聊——examples 已有 e2e，提升为黄金编号用例并纳入本集）；②限幅：ScriptedChatModel + 自定义工具回大结果 → 断言模型 prompt 中含「结果已截断：原始 N 字符」（seenPrompts 检视）；③索引：attachGlobal 无事件——SessionIndexObserver 不发事件；行为断言（spawn/chat/close/delete→index 状态序列）+ 断言 DELETED 联动（T113 后）。产 spec 34 §C + impl-92。
