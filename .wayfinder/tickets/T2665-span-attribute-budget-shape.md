---
id: T2665
title: span 属性预算审计的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

SpanAttributeBudget 的形状怎么裁决？（spec 1732 / effort #1732 / R33）（spec 1732 验收/裁决）

## Resolution

实例面 record(spanName, attrCount, attrBytes)+可调阈值默认 128 属性/8192 字节（OTel 默认对齐）+census(spans/overAttrLimit/overByteLimit/worstAttrs/worstBytes)——OTel 属性限额思想。
