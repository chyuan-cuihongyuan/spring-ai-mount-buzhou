# Spec 10046 — DecisionTreeCart CART 基尼树（effort #10046，X47）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10093–X10094，impl 2449）。
> 借鉴：Breiman 1984 思想——scikit-learn DecisionTreeClassifier 同源

## Problem Statement

轴平行判别树——贪心基尼分裂的分类基座面。

## Solution

DecisionTreeCart（core/metrics）：fit(double[][],int[],int,int,int)——每节点逐特征取排序去重相邻中点为阈值候选、加权基尼不升即裂（零增益允许——XOR 根节点面，sklearn 同口径；先到先得）；停机：纯/达 maxDepth/样本<2·minSamplesLeaf；叶取多数类（平局取首类）。

## Testing Decisions

一维阈值可分手锚全对+XOR 二维需深度 2 手锚全对+纯集直接叶+同值零候选转叶面+确定性+fail-fast 五面。

## Out of Scope

不做回归树（方差面另立）；不做剪枝面（代价复杂度另立）；不做随机森林集成。

## Further Notes

决策树奠基原语——Breiman CART；Wave 8 经典机器学习族收束件；开发勘误入档：严格下降裂不了 XOR 零增益根（sklearn 零增益可裂口径对齐）。
