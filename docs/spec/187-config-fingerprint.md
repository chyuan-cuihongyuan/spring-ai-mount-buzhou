# Spec 187 — 生效配置指纹（effort #212）

> wayfinder map：`.wayfinder212/MAP.md`（T559–T560）。延续 spec 175 SBOM 思想
> 到配置面——配置漂移可对账。

## Problem Statement

「环境 A 与 B 的 buzhou.* 配置差在哪」「上次发布后谁改了配置」——没有快照
机制时全靠人翻 yml 对眼睛：配置漂移是生产事故高频根因，且最难的恰是「发现
漂移了」这一步。

## Solution

`ConfigFingerprint`（core/config）：

- **构建**：`of(Map<String, String>)` —— 键排序 + 值 strip 归一 → 键级指纹表 +
  整体 summaryHex（一版配置一条锚，入部署记录/工单）。
- **对账**：`diff(other)` → `Diff(added, removed, changed)`（changed = 同键值异）。
- **脏输入**：null 键/值跳过（不炸指纹——提取侧质量不应炸审计侧）。
- 键集来源归宿主（Environment/binder 提取）——本类纯函数零 Spring 绑定。

## User Stories

1. 作为运维，两环境各拍指纹 diff——差集秒出，不用逐键对眼睛。
2. 作为发布流程，summaryHex 进部署记录——任何时点可答「当时配置是哪版」。
3. 作为审计，changed 键清单即「被改了什么」的直接证据。

## Implementation Decisions

- 与 ToolCatalogFingerprint（175）同构但独立类（口径不同：键值 vs 工具 schema
  ——不强行泛型抽象，两个小类比一个歪抽象好）。

## Testing Decimals

- 同 map 双构稳定；顺序无关；值空白差等价；diff 三分类；脏输入跳过；空 map 有锚。

## Out of Scope

- secret 脱敏；Spring 绑定；快照存档调度。

## Further Notes

- 对账三件套：工具目录（175）/ 配置面（本轮）/ API 面（既有快照测试）。
