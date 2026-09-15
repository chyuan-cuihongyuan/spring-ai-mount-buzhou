# Spec 1713 — 工具参数形态分布审计（effort #1713，R14）（effort #1713，R14）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2627–T2628，impl 1313，impl jq 类型分诊 / fail2ban 模式分类）。借鉴：工具入参是结构化 JSON、裸数值还是巨型 blob，决定限幅/裁剪/脱敏策略——形态无分布则策略无依据，blob 撞限幅才发现。

## Problem Statement

`ToolArgShapeAudit`（core/hook，实例面线程安全）：七态闭集 Shape（EMPTY/JSON_OBJECT/JSON_ARRAY/NUMERIC/BOOLEAN/LARGE_BLOB/PLAIN_TEXT）+record(String) 逐参分类计数+census()；超长阈值默认 4096 字符可调；classify(arg, threshold) 静态纯函数供只读探测。

## Solution

作为限幅调参者，LARGE_BLOB 占比 40% → 限幅/裁剪优先。

## User Stories

1. 17130
2. 17131
3. 17132

## Implementation Decisions

- 17133

## Testing Decisions

- 17134

## Out of Scope

- 17135

## Further Notes

- 17136
