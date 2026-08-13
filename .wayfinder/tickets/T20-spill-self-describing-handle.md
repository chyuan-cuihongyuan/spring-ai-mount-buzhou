---
id: T20
title: spill · 自描述 Reference Handle + token-aware 可配阈值
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T14
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#spill)

## What to build

溢出占位符（Reference Handle）统一含 **句柄 + 数据形状/schema 提示 + 字节/token 大小 + 精确回读动词与参数**，取代裸路径。溢出阈值改为**可配且按 token 计**（非硬编码字节/行），支持 per-tool 覆盖；任何截断必发**显式截断标记 + 回读句柄**，永不静默。

## Acceptance criteria

- [ ] 端到端：超大工具输出 → 上下文出现自描述占位符（含形状+大小+回读动词）→ 模型回读返回真实切片
- [ ] 阈值可配（token 计）、可按工具覆盖
- [ ] 截断时发显式标记 + 句柄（无静默截断）
- [ ] 既有 spill read_range 三模回读（byte/jsonpath/pagination）不回归

## Blocked by

无 —— 可立即开工。（T21、T22 依赖本片的占位符/阈值机制。）

## Resolution

已落地（Tier-1）。`SpillService` 占位符升级为自描述 Reference Handle：句柄 + **数据形状/schema 提示**（JSON 数组项数/对象顶层字段/文本行数 + 回读模式建议）+ **字符/token 大小**（4 字符/token 估算）+ 三模回读动词示例；预览截断显式标记（永不静默）。阈值 token-aware：全局 `thresholdTokens`（builder/yml，优先于 chars，×4 折算）+ per-tool `spillThresholdTokens`/`spillThresholdChars`；另修复视图级重入的幂等（同内容已落盘 → 复用占位符，保留对不同内容的一次调用一次落盘守卫）。spec 02 新增专节。测试：`SpillOffloadHookTest`（自描述/数组 item 形状/token 覆盖）、examples `SpillHotTailIntegrationTest`（会话接缝占位符+真实回读切片）。
