---
id: T973
title: fork 谱系游走环防护的形态裁决（命中率 getter ruled-out 后顺延）
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

原列主题「提示前缀缓存命中率 getter」缺口核查已被 spec 90 覆盖（Stats 四计数 + hitRate()）——ruled-out 顺延。fork 谱系（buzhou.fork.source 链）正常 fork 只造新会话成树——但**导入/还原路径**（spec 6）可能注入环状 SOURCE 链或超深链；面板/导出消费方游走谱系时无防护会死循环或 OOM。环防护怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 12 轮 = effort #711 / spec 711 / impl 514）：`ForkLineageWalker`（core/session）——`static Lineage walk(SessionStateStore, sessionId, maxDepth)`：沿 SOURCE 指针逐跳上溯，visited 集合判环（重复访问 = loopDetected，立即停）、maxDepth 封顶（depthCapped，默认常量 64）；返回 Lineage{ancestors 最近→根有序清单, loopDetected, depthCapped}——**只读不修**（检测面诚实语义；导入端校验如何处置留给导入方）。游走经 store 读，缺源即根（正常树零开销终止）。借鉴静态分析 call-graph 环检测（visited set + 深度上限双闸惯例）。
