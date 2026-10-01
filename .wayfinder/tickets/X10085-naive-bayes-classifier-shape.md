---
id: X10085
title: X 会话 43 NaiveBayesClassifier 多项式朴素贝叶斯 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

NaiveBayesClassifier（core/metrics）：fit(bagOfWords,labels,classCount) 词计数+拉普拉斯平滑 θ 对数化+predict(doc) 对数后验 argmax（平局取首类）；null/标签越界/负词 ID fail-fast。
