---
Type: task
Status: closed
---
## Question

turn 开始 MDC.put(buzhou.sessionId/turnSeq)，doFinally clear（try/finally 防残留）；虚拟线程下用内存 MDC（非 ThreadLocal 池化场景兼容）；日志样例验证与 OTel span 互查。验证：单测断言日志上下文含 sessionId、结束后清理。

## Resolution

spec 47 §A / impl-141 落地：chat/chatForEntity 轮次调用线程 MDC 关联（buzhou.sessionId /
buzhou.turnSeq 两键；doChat 拆 doChatTurn，外层 try/finally 包写/清——异常路径也清）。
**实现期裁定（诚实修正）**：stream 路径不做 MDC——诊断测试证实 Spring AI 流式管线把信号发射
切到 boundedElastic 线程，put 落订阅线程、remove 落发射线程（清错线程 = 订阅线程泄漏）；
ThreadLocal MDC 结构性限制，spec 47 §A 已同步修正并记录。测试钉住：chat 轮次内两键可见、
返回后必清、失败轮次也清、stream 路径不触碰 MDC。core 324 测试绿。
