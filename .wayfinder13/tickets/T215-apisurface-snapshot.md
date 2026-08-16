---
Type: task
Status: closed
---
## Question

api-surface 快照测试：反射扫描各模块 src/main 非 internal 包 public 类型全集 → 与
docs/api-surface.md 声明面比对；新增/移除公开类型未入档即测试失败（防意外 API 漂移）。

## Resolution

impl-179 落地：449 类型 × 14 模块黄金快照 + 比对测试 + regenerate 维护操作；实现期纠偏
Map 键结构 bug；reactor 形态门（单模块跑跳过，诚实边界）。T215 关闭。
