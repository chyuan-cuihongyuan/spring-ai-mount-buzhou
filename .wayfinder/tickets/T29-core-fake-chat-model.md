---
id: T29
title: core · FakeChatModel + record/replay 确定性测试基建
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

如何为 Buzhou 建「确定性单测 + 录制回放」测试基建，使并行工具、错误回喂、Turn 语义、恢复等行为可在不调真实 LLM 的前提下精确回归？事实源：Vercel AI SDK（26,168★ `MockLanguageModelV4`/`mockValues`）、Pydantic AI（19,271★ `TestModel`/`FunctionModel`/`capture_run_messages`）；Spring AI 9,299★ 不达标且官方无 fake（仅 evaluation 模块）——须自建。

## 待定决策（研究推荐已备）

1. `FakeChatModel implements ChatModel/StreamingChatModel` 持**脚本队列**（按调用序弹出，耗尽重复末值）；脚本项支持**单条 assistant 消息多个 toolCall**（并行回放关键语义）——采纳。
2. `RecordingChatModel` 装饰真模型，把 (request, response) 序列落 JSON fixture；回放按请求序列匹配，**失配即测试失败**（防静默漏断言）——采纳。
3. 录制 fixture 的存放与命名约定（`src/test/resources/recordings/`？）与脱敏（真实 key/PII 不落盘）——spec 定。
4. 是否提供 `ALLOW_MODEL_REQUESTS=false` 式全局防真实请求开关（Pydantic AI 模式）——建议给 examples 测试基类。

依据：`docs/research/oss-perfect-tier23.md` §2.2（1–2 周，ROI 高，**其余候选的回归地基、应最先做**）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) Implementation Decisions §core-1**（用户常设授权 2026-08-14 ratify、可推翻）。FakeChatModel 脚本队列（并行 toolCall 块）+ RecordingChatModel JSON fixture + 失配即测试失败；定位 Phase 0 地基。
