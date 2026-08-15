# 141 — MDC 会话轮次关联

**Parent:** spec 47 §A / [T172](../tickets/T172-mdc-correlation.md)

**What to build:** 轮次执行期间 SLF4J MDC 写入 `buzhou.sessionId`/`buzhou.turnSeq`，终结（含异常/取消）
必清；chat 路径覆盖调用线程（try/finally），stream 路径订阅建立写入、终结清除、未订阅不写入。

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] MDC 键命名空间 buzhou.*（sessionId/turnSeq 两键常量）
- [ ] chat 路径：doChat 拆 doChatTurn，外层 try/finally 包 MDC 写/清（异常路径也清）
- [x] stream 路径：**实现期裁定不做**——诊断证实 Spring AI 流式管线信号切 boundedElastic 线程，put/remove 落不同线程 = 订阅线程泄漏；结构性限制（spec 已修正）
- [ ] guard 拦截路径不写 MDC；未订阅流不写入
- [x] 测试：观察者回调（同线程）快照断言两键值正确；chat 返回后清除；失败轮次也清；stream 路径不触碰 MDC（边界钉住）
- [ ] buzhou-core `mvn verify -am` 全绿（含全量回归——MDC 泄漏会污染后续断言）
