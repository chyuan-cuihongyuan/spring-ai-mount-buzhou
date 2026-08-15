---
Type: task
Status: open
---
## Question

turn 开始 MDC.put(buzhou.sessionId/turnSeq)，doFinally clear（try/finally 防残留）；虚拟线程下用内存 MDC（非 ThreadLocal 池化场景兼容）；日志样例验证与 OTel span 互查。验证：单测断言日志上下文含 sessionId、结束后清理。
