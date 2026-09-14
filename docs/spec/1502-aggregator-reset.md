# 1502 — 计时聚合器双子实例清零面

> 来源：M 会话第 3 轮 = effort #1502（impl 1105）。Prometheus counter reset 语义 + 仓库规范「进程级静态读面须配 reset 注入点」的符合性补全。

## 背景

HookTimingAggregator（spec 647）与 ToolTimingAggregator 的 Holder.reset() 只把 current 置 null（关闭聚合）；聚合器实例的 timings 映射无清零面——enable 后 stats()/windowedMax() 只增不减：测试间基线污染（同 JVM 多测试类共享 Holder 无法重置断言基线）、长生命周期进程无法重建观测基线。

## 目标

- 两聚合器各补公开 `reset()`：清空 timings 映射；幂等；作用于实例（不碰 Holder）；未装配零副作用；
- Holder 的 enable/reset（置 null）既有语义零变化。

## 兼容性

纯新增方法，零行为变化；新公共 API 面（api 语义承诺——Javadoc 必备）。
