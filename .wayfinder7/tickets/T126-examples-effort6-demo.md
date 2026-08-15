---
Type: task
Status: open
blocked-by: T120
---
## Question

examples 新能力演示扩展（T98 同款口径）：多模态/导出导入/索引/限幅/字节摄取的演示用例（NewCapabilitiesDemoTest 增补或新 demo 类）？

## Resolution

AFK 自决：新 Effort6CapabilitiesDemoTest 五用例：①多模态 chat（ScriptedChatModel 断言媒体随轮下发 + 历史降级标记）；②导出→导入→续聊；③索引生命周期（spawn/chat/close→list）；④限幅（大结果工具→prompt 含截断提示）；⑤MediaIntake 字节→spill→MediaRef 闭环（T120 后）。产 impl-101。
