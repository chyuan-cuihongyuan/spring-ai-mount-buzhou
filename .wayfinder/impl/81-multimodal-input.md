# impl-81 — 多模态输入透传（MediaRef）

**What to build:** chat/stream/chatForEntity 携带媒体引用（URI 形态）直达模型；引用随消息
持久化；重发只随最新带媒体消息（旧轮降级文本标记）；token 估算媒体固定计费。

**Blocked by:** None（T106 已闭合）

**Status:** done

- [x] `MediaRef(mimeType, URI)`（core.session，公共 API 不泄漏 Spring AI 类型）+ `TOKENS_PER_MEDIA=320`
- [x] `AgentSession` 三 default 重载（UOE 显式失败）+ `DefaultAgentSession` 实现（PromptUserSpec.media）
- [x] `BuzhouChatMemory`：写路径 mediaRefs 落 metadata；读路径两遍扫描重发策略 + 降级标记
- [x] `CharHeuristicTokenEstimator`：媒体固定档位计费
- [x] 测试：e2e 四用例（下发/持久化/重发策略/纯文本回归）+ 估算差分 + 入参校验
- [x] spec 27 新篇

## Done

commit：见 git log（impl-81）。验证：buzhou-core 273/273 绿。
