---
id: T55
title: 生产级两轮研究——本地缺口勘察 + ≥10K★ 运维实践核验
type: research
status: closed
assignee: ""
blocked-by: []
created: 2026-08-14
---

## Question

以 GitHub stars ≥ 10K 的开源项目为事实源，核验「生产级运行时库」应具备的工程实践；同时全量勘察 buzhou 四机制（core/memory/spill/guard + store-jdbc/redis）的生产化缺口，产出可裁决的采纳映射。

## Resolution

**6 并行子 agent 完成（2026-08-14）**：2 本地勘察（core 8 维度、五模块 8 维度）+ 4 外部研究（Spring Boot/Framework 库工程、数据基础设施运维、运行时可靠性、安全运维+LLM serving）。产出 [docs/research/oss-production-grade.md](../../docs/research/oss-production-grade.md)。

**关键结论**：
- 四机制功能完备但缺七类系统性外围防护（停机/挂起/双主/只进不出/事务/可诊断/配置），证据见研究文档 §0 表。
- 载荷性本地结论五项已复核证实：MySQL 索引无 IF NOT EXISTS（二启必炸）、JdbcToolCallLog/JdbcRunRegistry 未装配、renew() 有 SPI 无调用方、LeaseLostException 零抛出、AuditChain 未进自动装配。
- 五条数据治理公理（封闭才计时/声明式+低频兑现/阈值四件套/压缩≠回收/配额分可牺牲集合）+ Spring 生命周期/配置/诊断形状 + Netty/Kafka/gRPC 可靠性形状 + Vault/OPA/Deno 运维形状全部入档。
- 纠正：Vault audit device 非哈希链；JMH/Reactor/SLSA 不达标仅注记。
