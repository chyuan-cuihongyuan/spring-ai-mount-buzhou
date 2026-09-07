# Spec 329 — API 快照收口（effort #329）

> wayfinder map：`.wayfinder329/MAP.md`（T649–T650）。C 会话半程收口轮。

## Problem Statement

公共面防线（T215 快照门）在 Windows 本机形同虚设：classpath 按 `:` 硬切
（Windows 分隔符是 `;`）、`\classes` 不匹配 `/classes`——比对门恒跳过，
regenerate 会把黄金快照写空。同时公共面自 effort #143 后未入档：B 尾巴 +
C 会话 R1–R29 已攒 38 个公开类型，快照与 api-surface.md 双双落后。

## Solution

- 快照机跨平台修复：classpath 读取统一 `File.pathSeparator` 切分 +
  `\`→`/` 归一（比对门、模块名判定、目录扫描 relativize 三处共用）；
  Linux 行为零变化（CI 幂等）。
- `regenerateSnapshot` 全量再生：+38 型（运维弧线/告警观测/演练事故/
  失控防护/流量共享族/B 尾巴）。
- api-surface.md 补「effort #300–#328」一节（按主题分组入档——不逐轮
  切片）。
- 比对测试本机（Windows reactor -am）真跑且绿——防线首次双平台生效。

## User Stories

1. 作为贡献者（Windows），我想本机也能跑快照比对与再生，所以公共面
   防线不再只在 CI 生效。
2. 作为审计者，我想让 38 个新公开类型进黄金快照与 api-surface.md，所以
   稳定性政策（CONTRIBUTING）有完整事实源。
3. 作为 CI，Linux 上的比对行为必须与修复前完全一致，所以幂等性不破。

## Implementation Decisions

- 只动测试基建，零生产面变化。
- 归一化收口为 `normalizedClasspath()`/`splitClasspath()` 两个私有助手——
  三处消费点共用，防止下次再漏一处。

## Testing Decisions

- 修复的验收即比对测试自身：Windows reactor 下 `Skipped: 0` 且绿；
  regenerate 后 diff 恰为 38 行新增（人工核对入 commit message）。

## Out of Scope

- 方法级快照；yml 键总清单；api-surface.md 历史节重写。

## Further Notes

- C 会话半程（30/50）：公共面防线补齐后再走后半程，新类型随轮入档
  不再攒量。
