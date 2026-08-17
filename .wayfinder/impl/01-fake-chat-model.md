# 01 — core · FakeChatModel + record/replay 确定性测试基建

**What to build:** 不调真实 LLM 即可确定性回归完整 agent session：脚本化假模型按调用序回放响应（含单条消息多 toolCall 的并行块），录制装饰器把真实会话落 JSON fixture、回放失配即测试失败。这是 spec 12 全部后续切片的测试地基。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：core testsupport 五件套 + FakeChatModelTest 5 例 + examples FakeModelGuard + FakeChatModelEndToEndTest 3 例 + ToolErrorFeedbackIntegrationTest 迁移（断言不弱化）；spec 05 增「测试基建」节）

- [ ] `FakeChatModel`（ChatModel + StreamingChatModel）：脚本队列按调用序消费、耗尽重复末值；脚本项支持单条 assistant 消息多个 toolCall（并行回放语义）；流式可配 chunk 延迟
- [ ] `RecordingChatModel` 装饰真模型：(request, response) 序列落 JSON fixture（真实 key/PII 不落盘），fixture 约定 `recordings/` 资源目录
- [ ] 回放按请求序列匹配，失配即测试失败（防静默漏断言）
- [ ] examples 既有端到端测试至少一条迁移到 FakeChatModel 驱动，断言不弱化
- [ ] 测试基类提供「防真实请求」全局开关（未注册 fake 时调真模型即失败）
- [ ] spec 05（并行工具）或测试约定文档同步该基建说明

> spec 12 §core-1；wayfinder2 [T29](../tickets/T29-core-fake-chat-model.md)。源：vercel/ai 26,168★ + pydantic-ai 19,271★（Spring AI 9,299★ 不达标无官方 fake）。
