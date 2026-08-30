---
id: T68
title: 综合 · 生产级收口范围切定与优先级
type: grilling
status: closed
assignee: ""
blocked-by: [T56, T57, T58, T59, T60, T61, T62, T63, T64, T65, T66, T67]
created: 2026-08-14
---

## Question

十二个方向的裁决如何切为可执行的 Phase 与切片？需裁决：优先级排布（致命缺陷先行：挂起/停机/MySQL 迁移 → 正确性：事务/续租 → 治理：清理/保留/配额 → 运维：guard 闭环/可诊断 → 配置/测试基建收口）、切片粒度（/to-tickets 的输入）、与既有测试套系的回归关系。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13「范围与阶段」**：Phase 0 地基（Deadline+挂起修复、FaultInjecting）→ 1 致命（停机、schema 迁移+装配）→ 2 正确性（事务、续租 fence）→ 3 治理（清理保留、配额+缓存）→ 4 运维闭环（guard、可诊断）→ 5 配置收口（校验/元数据/默认值 + 装配矩阵收口）。切片粒度交 /to-tickets；回归门槛 = 既有 576 tests 全绿。
