# Spec 1892 — 阶梯加压计划（effort #1893，R93）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2985–T2986，impl 1493）。借鉴：
> k6/Gatling（25K+ 星级）ramping stages 语义——压力按台阶线性爬坡
> （目标 VU + 台阶时长），压测负载曲线先声明后执行；台阶内线性
> 插值、台阶末保持。

## Problem Statement

压测负载曲线拍脑袋：突发放满 VU 把冷启动当性能问题、爬坡无计划
让采样点对不上负载档位——「第 15 秒的 P99 对应多少并发」没有
确定性计算面。

## Solution

`RampProfile`（core/policy，静态纯函数 + 嵌套 Stage）：

- `targetAt(stages, elapsedMillis)`：台阶内从上一目标线性爬向本
  目标（插值）；全部台阶走完保持末目标；
- `totalDuration(stages)`：总时长；`peakTarget(stages)`：峰值目标；
- 校验：台阶时长 ≥ 1、目标 ≥ 0、非空 fail-fast。

## User Stories

1. 作为压测作者，台阶 [(0,10s),(50,10s),(20,10s)]：第 5s 目标 25、
   第 15s 目标 40——采样点与负载档位一一对应。
2. 作为报告评审者，totalDuration/peakTarget 声明即可核——曲线
   先声明后执行。
3. 作为稳态观察者，末台阶后保持 20——稳态段采样有锚。

## Implementation Decisions

- 纯函数零状态；首台阶从 0 起爬；elapsed 超总时长保持末目标
  （不越界）；负 elapsed fail-fast。

## Testing Decisions

- 三台阶计划五采样点（爬坡/反爬/稳态/超时保持）；峰值与总时长；
  畸形三型（空表/零时长台阶/负目标）fail-fast。

## Out of Scope

- 不做真实负载生成（归压测工具）；不做自适应调压。

## Further Notes

- 与 ChaosBudgetGate 允许窗互补：那是混沌允许窗，这是压测负载
  曲线声明。
