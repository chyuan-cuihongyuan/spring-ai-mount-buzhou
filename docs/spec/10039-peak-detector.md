# Spec 10039 — PeakDetector prominence 峰检测（effort #10039，X40）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10079–X10080，impl 2442）。
> 借鉴：SciPy find_peaks 思想——信号处理同源

## Problem Statement

信号峰提取——不止看高度、更看显著度（对基座不敏感）的 robust 峰面。

## Solution

PeakDetector（core/metrics）：findPeaks(double[],double)——严格局部极大（两邻严格小，平台无峰明示）；prominence：自峰向左/右爬升至更高峰或边界，各取段内谷底，prominence=峰高−max(左谷底,右谷底)；≥阈值入列（索引升序）。

## Testing Decisions

双峰手锚（prominence 5 与 3 逐峰手锚）+阈值过滤面（3.5 只留高峰）+单调/平台无峰+随机低幅噪声淹没脉冲检出+确定性+fail-fast 五面。

## Out of Scope

不做 distance/width 附加过滤（SciPy 参数面按需另立）；不做谷检测镜像面；不做边界峰（半窗语义无定义）。

## Further Notes

SciPy find_peaks 的 prominence 核心面复刻；Wave 7 信号与频谱族第四件。
