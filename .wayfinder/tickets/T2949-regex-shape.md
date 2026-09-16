---
id: T2949
title: 正则风险审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

工具/护栏正则的 ReDoS 形态怎么审？（spec 1874 / effort #1874 / R75）

## Resolution`

**OWASP ReDoS/SafeRegex 惯例启发式 `RegexRiskAudit`（buzhou-guard）**：
audit → 三形态字符级扫描（嵌套量词/量词组内交替/重叠交替，转义感知）
分级 SAFE/SUSPECT（单形态）/DANGEROUS（多形态叠乘）；findings 可解释。
诚实边界：静态启发式非完备（automata 完备分析归未来）。纯扫描不拦截。

