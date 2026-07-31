# 悬空调用修复规则

Type: grilling
Status: open
Blocked by: 06

## Question

悬空调用（dangling tool call）的检测与修复细则：什么算悬空（assistant 消息含 tool_calls 但后续无对应 tool 结果）？加载历史时的修复策略——直接剔除整条违规消息，还是补一条合成结果消息（各模型 API 对孤儿 tool_calls 的容忍度不同）？修复动作是否记录到可观测层？跨实例接管时如何区分"悬空"与"另一实例正在执行中"（租约/心跳）？
