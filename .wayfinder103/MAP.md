# Wayfinder Map — Buzhou 模型端点离群驱逐（effort #103，B 会话第 15 轮）

> B 会话第 15 轮。主题池「端点离群驱逐」：延迟感知排序（spec 64）让快者优先，
> 但持续 5xx 的坏端点仍会被轮到试探。借鉴 Envoy outlier detection——连续错误
> 端点直接逐出池子，窗口过后再回来。

## Destination

ModelOutlierEjection（resilience/fallback）：连续 N 错驱逐 ejectionWindow，
窗口过自动复池；filter(candidates) 从降级链候选中剔除被逐者；success 复位
连错。与 spec 15 熔断（单模型状态机）/64 排序（快者优先）正交——这是
「池成员资格」面。

## Notes

- 号段：B=奇数 spec（本轮 149）。
- 与模型熔断的区别：熔断是「主模型这条路的开关」，驱逐是「备选池的成员资格」。

## Decisions so far

- 驱逐只看连续错误数（延迟离群留档 Not yet specified）。

## Not yet specified

- 延迟 p99 离群驱逐；驱逐比例上限（Envoy max_ejection_percent）。

## Out of scope

- 沿用 #7–#102；跨实例共享驱逐状态。

## Tickets

- [x] [T505 ModelOutlierEjection（连错驱逐/窗口复池/过滤）](tickets/T505-outlier-ejection.md)（impl-287）
- [x] [T506 驱逐回归（连错逐出/窗口复池/复位/过滤/隔离）](tickets/T506-outlier-tests.md)（impl-287）
