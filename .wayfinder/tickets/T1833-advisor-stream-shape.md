---
id: T1833
title: R13 形态——ObservabilityAdvisor 流式路径分支补测（harness 从零搭建）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 13 轮：ObservabilityAdvisor 流式路径（68 missed 集中区）的 harness 从零搭建方案与补测面？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 13 轮 = effort #1212 / spec 1212 / impl 915）：

1. **harness 方案（javap 实证 Spring AI 2.0.1 构造面）**：StreamAdvisorChain 匿名 stub（nextStream/getStreamAdvisors/copy 三方法）+ ChatClientRequest(Prompt, Map) 2 参 record + ChatClientResponse(ChatResponse, Map) + ChatResponse(Generation|List, ChatResponseMetadata.builder().usage(DefaultUsage)) + ChatGenerationMetadata.builder().finishReason()；recorder 用 RecordingBase extends BaseSpanRecorder（doEnqueue 捕获 PendingSpan/PendingEvent，super(MicrometerDualWriter, false)）。
2. **补测面（7 用例）**：happy path（TTFT 打点/THINKING 聚合/FINAL_REPLY/usage/TPOT/finish_reason 全断言）、usage-only 无首信号、sawToolCalls 抑制 FINAL_REPLY、completionTokens=null 跳 TPOT、流错标 ERROR 且异常透传、take(1) 取消标 CANCELLED、resolveTurnParent turn→session→null 三级回退。
3. **隔离注记**：事件断言经 PendingEvent instanceof 过滤（PendingSnapshot 因 snapshotCapture=true 同场入队）；TokenEstimator 三方法匿名 stub（非函数接口）。
4. **边界**：不改主代码；ObservabilityAdvisor 其余分支（firstMatch/captureInjectionSnapshot 细节）视复扫余量决定。
