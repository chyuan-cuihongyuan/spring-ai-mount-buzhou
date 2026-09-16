---
id: T3140
title: QoS 资源声明分级的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3139]
created: 2026-09-17
---

## Question

QosClassifier 合同（三态/混维拉低/驱逐序/让位/畸形）怎么钉住？（spec 2019 / effort #2019 / R20）

## Resolution

**七用例全绿**（buzhou-core；方法名空格笔误一次编译红修正后 7/7）：
全维保额 G / 全零与空列表 BE / request<limit B / 混维拉低三例
（G+零声明→B、G+B→B、纯上限→B）/ 驱逐序 0<1<2 / 让位四象限 /
矛盾（10>5）与负声明、null rank fail-fast。
