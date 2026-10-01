# Spec 10042 — NaiveBayesClassifier 多项式朴素贝叶斯（effort #10042，X43）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10085–X10086，impl 2445）。
> 借鉴：McCallum–Nigam 1998 思想——scikit-learn MultinomialNB 同源

## Problem Statement

词袋文本分类——条件独立假设下的对数线性生成面。

## Solution

NaiveBayesClassifier（core/metrics）：fit(int[][] docs, int[] labels, int classCount)——类词计数+Laplace α=1 平滑 θ=(count+1)/(total+V) 全对数化防下溢+先验对数；predict 对数后验 argmax（平局取首类确定性口径）；不可变模型实例。

## Testing Decisions

双类词袋手锚（特征词完全判别全对）+未见词平滑面（新词不炸且不翻盘）+类先验倾斜面（不均衡语料偏先验）+确定性+fail-fast 四面。

## Out of Scope

不做 Bernoulli/Gaussian 变体（另立）；不做停用词/TF-IDF 预处理（上游面）；不做增量 fit。

## Further Notes

文本分类生成式基座；Wave 8 经典机器学习族首件。
