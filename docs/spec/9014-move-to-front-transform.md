# Spec 9014 — Move-to-Front Transform 前移变换（effort #9014，W15）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9029–W9030，impl 2367）。
> 借鉴：Move-to-Front（Bentley 1986——bzip2 BWT 后半管线同源）

## Problem Statement

聚簇列直接熵编码的病：局部性红利吃不到——
**MTF**：表内位次输出+移表首，聚簇→小位次
偏斜分布。

## Solution

MoveToFrontTransform（core/message，静态工具面）：
encode→位次序列；decode 同构重建精确逆；256 表
初始序；确定。

## Testing Decisions

{2,1,0}/{0,1,2}/重复字节手锚；聚簇平均位次<64
vs 全谱对照；4 文本+60 随机往返；确定性；
fail-fast。

## Out of Scope

不做熵编码后端（消费方组合）；不做自适应
权重表变体；不做流式。

## Further Notes

与 BWT（9013）组成 bzip2 前半管线。Wave 3 第三件。
