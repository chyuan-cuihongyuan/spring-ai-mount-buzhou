---
id: T54
title: spec 12 范围切定与优先级（「做完美」收口）
type: grilling
status: closed
assignee: ""
blocked-by: T29, T30, T31, T32, T33, T34, T35, T36, T37, T38, T39, T40, T41, T42, T43, T44, T45, T46, T47, T48, T49, T50, T51, T52, T53
created: 2026-08-14
---

## Question

「做完美」的定义收口：Tier-2 全量 + Tier-3 精选之中，**哪些进 spec 12 建造范围、哪些留 Out of Scope**？各模块落地顺序与里程碑如何切？（此票 = `/to-spec` 合成 spec 12 前的综合裁决；被 T29–T53 全部决议喂入。）

## 裁决框架（研究推荐已备）

1. **必进**（高 ROI + 地基）：T29 测试基建、T30 参数校验重试、T31 CancelMode、T32+T33 恢复双票、T36 evictRatio、T37 sleep-time、T38 memory 工具+防投毒、T43 head+tail、T44 context-clearing、T45 chunk hash、T48 红队门、T49 FIDES taint、T50 审计链、T52 内嵌策略子集。
2. **应进**（中高 ROI）：T39 保真 eval、T40 压缩检查点、T41 三模搜、T34 interrupt/resume+fork、T46 语义回读。
3. **按需/可选**（接口预留、实现按部署需求）：T51 Firecracker/E2B 档、T53 ONNX 分类器、T47 AST 切片、T42 episodic。
4. spec 12 须同步修订机制 Spec（01 记忆压缩 / 02 Spill / 05 并行工具 / 07 Hook 护栏，可能新增恢复/审计章节）并守住测试哲学（外部行为、最高接缝、FakeChatModel 基建）。

依据：`docs/research/oss-perfect-tier23.md` §6 汇总表 + 各模块实施顺序节。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) 全篇（范围切定）**（用户常设授权 2026-08-14 ratify、可推翻）。Tier-2 全量+Tier-3 精选全数入范围；必进 15/应进 5/按需 6 排 Phase 0–6；Firecracker/E2B 实现与 FIDES 二期出界。
