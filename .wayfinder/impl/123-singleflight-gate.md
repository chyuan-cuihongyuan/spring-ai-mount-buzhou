# impl-123 — 会话单飞闸

**What to build:** 同会话并发第二轮次确定拒绝（TURN_IN_FLIGHT）；终结释放闸。

**Blocked by:** None

**Status:** done

- [x] ErrorCode.TURN_IN_FLIGHT（NON_RETRYABLE）
- [x] DefaultAgentSession CAS 0→1 单飞闸（三入口）+ javadoc 冷流订阅契约注记
- [x] 测试：在途拒绝（chat/stream 两入口）/完成后释放/异常收尾释放——core 绿
