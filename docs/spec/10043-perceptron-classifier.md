# Spec 10043 — PerceptronClassifier 感知机（effort #10043，X44）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10087–X10088，impl 2446）。
> 借鉴：Rosenblatt 1958 思想——scikit-learn Perceptron 同源

## Problem Statement

线性二分类的最简判别面——错分驱动的在线超平面学习。

## Solution

PerceptronClassifier（core/metrics）：fit(double[][], int[], double lr, int epochs)——逐样本 ŷ=[w·x+b≥0]、错分更新 w+=lr(y−ŷ)x、b+=lr(y−ŷ)；样本序固定遍历（确定性口径）；不可变模型。

## Testing Decisions

AND 门手锚（训练集全对）+OR 门手锚+单步更新数值手锚（w/b 逐位对拍手算）+确定性+fail-fast 四面。

## Out of Scope

不做 voted/averaged 感知机变体（另立）；不做核化面；不做学习率衰减调度。

## Further Notes

神经网络的起点原语——线性判别的锚定件；Wave 8 经典机器学习族第二件。
