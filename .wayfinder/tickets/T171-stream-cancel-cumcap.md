---
Type: task
Status: closed
---
## Question

流取消原因分类计数 buzhou.stream.cancelled{reason=client|deadline|guard}（DefaultAgentSession doFinally + ObservabilityAdvisor doOnCancel 归一）；流累计时长上限可配（默认 10min，超限取消并记 reason=deadline）——修慢滴流无累计上限的注释自认边界。验证：分类计数 + 超限取消路径单测。

## Resolution

spec 46 §B / impl-140 落地：buzhou.stream.cancelled{reason=client|deadline|guard} 分类计数
（client=doFinally CANCEL / deadline=TimeoutException 或 StreamTotalTimeoutException 经 doOnError /
guard=beforeTurn 拦截）；慢滴流累计上限 buzhou.core.stream-total-timeout（缺省 10m、≤0 关闭——
实现期自 spec 草案 buzhou.session.* 修正为 buzhou.core.* 与既有旋钮同族）经
takeUntilOther(delay→标记异常) 以 onError 终结并复用 failTurnOnce 链路。
诚实边界入档：超限触发时上游 span 包装收到 cancel 信号，MODEL_CALL span 终态可能记 CANCELLED；
TURN 级记账不受影响。HarnessAssembler.withStreamTotalTimeout wither + DefaultAgentSession
16 参构造（兼容重载保留）+ BuzhouCoreProperties.Core 第 4 键。core 321 测试绿（4 新测试：
慢滴截断/客户端取消/护栏拦截/关闭回归）。
