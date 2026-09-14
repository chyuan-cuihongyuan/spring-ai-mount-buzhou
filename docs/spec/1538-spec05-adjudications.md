# 1538 — spec 05 判定项批量回写（F3/F4/F6）

> 来源：M 会话第 42 轮 = effort #1538（impl 1141）。

## 裁定

- F3：注入通道实现定案 per-session 组装（BoundedToolCallingAdvisor 随会话构造）——Builder Bean 通道不采用（与租约/隔离舱会话级机制对齐）；
- F4：`buzhou.parallel.*` 键族未实现——键表按实现重写（buzhou.core.tool-timeout / tool-transient-retry / tools.serial-groups / tool-batch-response-budget）+ config-reference 指针；并发 8 为编程式默认入档；
- F6：DbPolicyConfigProvider 退避为 DoubleSupplier 纯函数实现——spec 回写实现口径，注入面不补。

## 兼容性

纯文档回写零行为变化。
