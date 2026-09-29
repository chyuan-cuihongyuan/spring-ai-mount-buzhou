---
id: V8029
title: V 会话 V15 MinWindowSubstring 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

多重需求覆盖窗口怎么均摊线性？（spec 8014 / effort #8014 / V15）

## Resolution

**MinWindowSubstring（core/metrics）**：need/window 双计数
表+satisfied 双指针伸缩；并列取最左 canonical；无覆盖空串
诚实缺省。（勘误：原拟 QuotientFilter 退回雾区——删除路径
簇边界超时预算，U24 同款纪律。）
