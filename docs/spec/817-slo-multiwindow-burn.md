# 817 — SLO 多窗燃烧率联合判定

> 来源：H 会话第 18 轮 = effort #817 / [T1135](../../.wayfinder/tickets/T1135-slo-multiwindow-burn.md) / [T1136](../../.wayfinder/tickets/T1136-slo-multiwindow-burn-verify.md) / impl 570。
> 借鉴：Google SRE Workbook multi-window multi-burn-rate（SloTH/pyrra 思想）。

## Problem

单窗 burnRate 超阈即响：流量毛刺（快窗尖峰）与慢性渗漏（慢窗缓慢烧）都触发——误报多。「快窗确认冲击、慢窗确认持续」的双窗共振判定缺位。

## Solution

`SloMultiWindowBurn`（core.health，纯函数）：

- **共振判定**：fastBurn ≥ fastThreshold 且 slowBurn ≥ slowThreshold 且双窗样本 ≥ minSamples → incident=true。
- **诊断 reason**：快窗独热=疑似毛刺、慢窗独热=慢性渗漏、不足=不判、双冷=安静——每条判定带人话解释。
- **组合式**：两窗由调用方各自配置 ErrorBudget 窗长后取 burnRate/样本喂入（ErrorBudget 零变更）。

## 兼容性

纯新增静态工具；ErrorBudget/AlertGate 零变更。

## 诚实边界

不持时间窗不触发通知（判定脑单一职责）；边界 ≥ 含等号（Workbook 口径）；阈值语义对称（非 fast>slow 强约束——由调用方配置保证）。
