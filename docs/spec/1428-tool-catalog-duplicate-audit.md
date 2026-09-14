# 1428 — 工具目录重名审计

> 来源：L 会话第 29 轮 = effort #1428（票 T2157 / T2158 / impl 1081）。借鉴：Spring 容器 bean 重名 fail-fast / Maven Enforcer duplicate classes 检查（静默遮蔽是装配事故的温床）。

## Problem Statement

`HarnessToolCallingManager` 以 HashMap 按 `toolDefinition.name()` 建索引——**重名工具静默互相覆盖**（后注册者胜，先注册者被遮蔽且无任何信号）。本地工具与多台 MCP server 工具天然可能同名（read_file 两边都有是常态）：遮蔽导致「这次调用到底调的是哪个实现」不可解释，且随装配顺序漂移。

## 目标

- `ToolCatalogDuplicateAudit`（core/exec，纯函数静态面，private 构造）：
  - `analyze(List<String> toolNames)` → `record Report(totalTools, distinctTools, duplicates, duplicateCount)`；
  - `DuplicateGroup(toolName, count)`：仅列 ≥2 同名组，名字典序；
  - 空清单零报告哨兵；纯只读不裁决（修复 = 重命名或显式排除——归宿主装配）。
- 审计对象为名字清单（调用方从 callbacks/registry 抽取）——不绑定义耦合。

## 兼容性

纯函数零 IO；不改 HarnessToolCallingManager 装配语义（ HashMap 覆盖行为保持——审计面只是显形，行为修复另轮决策）。

## Out of Scope

- 重名时 fail-fast/显式报错（行为变更需全链回归，另轮）。
- MCP server 前缀合成命名（namespace 化方案另轮）。
- 按来源（local/mcp-<server>）分组统计（无来源信息的名字清单口径）。
