---
Type: task
Status: closed
---
## Question

同一 sessionId 并发 chat/stream 属「未定义使用」（仅文档声明）：是否提供框架级 per-session 单飞闸？第二个并发 turn 排队还是快速失败？错误面如何暴露？

## Resolution

AFK 自决：是，默认开启无关闭开关。DefaultAgentSession 在途计数升级 CAS 0→1 单飞闸（chat/chatForEntity/stream
三入口同闸）；第二并发轮次立即抛 BuzhouException(ErrorCode.TURN_IN_FLIGHT)（NON_RETRYABLE 新码），
消息携带 sessionId 与处置指引；轮次终结（含异常收尾/流式 doFinally/cancel）释放。跨进程仍归租约门。
排队/合并显式拒绝（与确定性目标相反）。产 spec 40 §B + impl-123。
