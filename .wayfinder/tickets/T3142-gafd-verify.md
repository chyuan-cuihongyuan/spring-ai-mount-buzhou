---
id: T3142
title: 冷启动豁免 φ 门的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3141]
created: 2026-09-17
---

## Question

GraceAwareFailureDetector 合同（三态/滤噪/毕业/畸形）怎么钉住？（spec 2020 / effort #2020 / R21）

## Resolution

**七用例全绿**（首跑 1 红根因：测试只喂两拍 φ 样本不足恒 0 且沉默
不够远——三拍建模+5 均值沉默修正后 7/7）：毕业后高 φ CONFIRMED /
豁免期失败 φ 零样本恒 0 且豁免账 3 / 窗外三拍建模远沉默 CONFIRMED
且计账 3 / 健康节奏 HEALTHY / 首拍毕业毕业后直接计账 / 主路径三态
完备 / 畸形四型 fail-fast。
