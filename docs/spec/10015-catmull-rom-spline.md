# Spec 10015 — CatmullRomSpline 样条（effort #10015，X16）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10031–X10032，impl 2418）。
> 借鉴：Catmull–Rom 样条（Catmull & Rom 1974——THREE.js/游戏引擎相机轨迹同源）

## Problem Statement

路径平滑/相机轨迹需过控制点的插值
样条——贝塞尔不过控制点——张量
0.5 张力过点插值。

## Solution

CatmullRomSpline（core/policy，实例面）：
point(t)——均匀参数化四点三次插值，
过全部控制点 C¹ 连续；端点虚拟重影
（首尾复制）；参数域 [0,n−1] 越域/
null/点数 <2/非二维 fail-fast。

## Testing Decisions

过点圣像（5 控制点逐点 1e-9）；共线
退化（中段线性+全域 y≡0）；两点直线
中点手锚；确定性双跑；参数越域 fail-fast。

## Out of Scope

不做弦长参数化（ centripetal 变体另立）；
不做 B 样条逼近面（不过点——BezierCurve
9034 已占异面）；不做样条求导/弧长。

## Further Notes

与 BezierCurve（已占）同域不同面：过点
插值 vs 控制多边形逼近。
