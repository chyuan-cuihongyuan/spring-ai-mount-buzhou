# 10 — 读写护栏 Hook

**What to build:** SpillOffloadHook（afterTool，数组逐条独立判定、单条失败仅该条降级）替换结果为引用句柄；写侧 @LongContentParam+xxxPath 互补参数协议（beforeTool 校验白名单路径→加载全文→REPLACE 覆盖内容参数并剥离路径字段，下游防御性 log.warn）；副本分离默认拦截（直改只读源被拦并提示 copy_file）+copy_file/str_replace 工具；失败非对称：读 CONTINUE 降级+告警 Event、写 BLOCK 阻断+Error Event。

**Blocked by:** 08, 04

**Status:** ready-for-agent

- [ ] 长结果经 Hook 换引用句柄，模型上下文只见句柄（端到端）
- [ ] 模型只传 xxxPath 时工具拿到全文（且路径参数被剥离）
- [ ] 直改只读快照被拦截；copy_file 后 str_replace 成功
- [ ] onload 失败阻断调用；offload 失败透传原文不阻断
