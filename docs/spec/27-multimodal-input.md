# Spec 27 — 多模态输入透传（MediaRef）

> effort #6（T106 / impl-81）。对齐 OpenHands 截图理解、aider 附图改码、Dify 多模态节点
> 的输入面标配；Spring AI `UserMessage.media` 原生通道。

## Problem Statement

`AgentSession.chat(String)` 与消息模型全链路纯文本——生产 agent 常见的「文本 + 图片/PDF」
输入无法表达，模型侧能力（多模态理解）被 harness 输入面封死。

## Solution

`chat(input, List<MediaRef>)` / `stream` / `chatForEntity` 三入口透传媒体引用
（`MediaRef(mimeType, uri)` URI 形态）；媒体引用随消息持久化（metadata
`mediaRefs`，store JSON 列自动序列化）；内存视图**重发策略**——媒体只随最近一条带媒体
的用户消息重发，更早轮次降级为文本标记（token 成本可控，store 全量保留可回溯）；
token 估算按每媒体固定 320 计（预算闸诚实累计）。

## User Stories

1. As a 应用开发者, I want chat 携带图片引用让模型看图, so that 截图理解/附图改码场景可用。
2. As a 应用开发者, I want 媒体引用持久化, so that 重启/续跑后历史媒体可回溯。
3. As a 平台运维, I want 旧媒体不每轮重发, so that 多媒体长会话 token 成本不失控。
4. As a 平台运维, I want 媒体计入 token 预算估算, so that 预算闸对多媒体输入诚实生效。
5. As a 应用开发者, I want 不支持多模态的实现显式报错, so that 媒体不被静默丢弃。

## Implementation Decisions

- **API 形态**：`MediaRef(mimeType, URI)` 值对象（core.session）；三入口 default 抛
  UOE（显式失败，不静默丢）；`DefaultAgentSession` 实现——PromptUserSpec.media 组装。
- **URI-only**：字节直传不入 API（base64 塞消息体膨胀存储）；字节由应用侧落对象存储/
  spill 后以 URI 引用（fog：spill 化字节摄取助手）。
- **持久化**：`BuzhouMessage.metadata["mediaRefs"]`（[{mimeType, uri}]）——JDBC/Redis
  metadata JSON 列随既有序列化路径，无 schema 变更。
- **重发策略**：内存视图两遍扫描——最近一条带媒体消息附 Spring Media 重发；更早的
  content 追加 `[历史媒体（本轮未随附）] mime uri` 标记（确定性变换，每轮一致）。
- **token 口径**：每媒体固定 320（`MediaRef.TOKENS_PER_MEDIA`；尺寸未知按中位图片档位估）
  计入 `CharHeuristicTokenEstimator.estimateMessages`。
- **REASK 兼容**：`chatForEntity(input, media, type)` 两次尝试全程携带媒体。
- **压缩边界**：微压缩只回收 TOOL 消息（既有口径），带媒体用户消息天然不进回收。

## Testing Decisions

- e2e（ScriptedChatModel 记录 Prompt）：①媒体随本轮 UserMessage 下发 + store metadata
  持久化；②第二轮重发只随最新媒体消息、旧消息降级标记；③纯文本回归零差异；
- 估算差分断言：带媒体与纯文本消息 estimateMessages 差 = 320×n；
- 入参校验：空 mime/null uri 构造期 IAE。
- 先例：`SessionForkEndToEndTest`。

## Out of Scope

- 音频/视频媒体（Spring AI Media 面向图片/PDF；音视频档位待生态成熟）。
- 字节直传 API（URI-only；spill 化摄取助手记 fog）。
- 模型侧媒体输出（图像生成等）。

## Further Notes

- MediaRef 公共 API 不泄漏 Spring AI 类型（Media 组装在 core 内部完成）。
- 与 spec 19（结构化输出）正交：REASK 轮媒体不丢。
