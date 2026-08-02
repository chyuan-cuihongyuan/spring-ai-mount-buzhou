# 13 — Hook→state→Attachment 闭环

**What to build:** 通用事实模型（key/value/producer/createdTurn/ttl）+fact.*/auth.* 命名空间；FactCollector 三要素脚手架（判定器/渲染器/ttl）；注入视图构建时未过期事实渲染为 system-reminder 块插近期原文前（摘要块在前、事实块随后）；事实写入摘要 Current State 段；注入 token 计系统侧固定扣除。

**Blocked by:** 07

**Status:** ready-for-agent

- [ ] 注册一个采集器后，工具调用触发的事实下一轮出现在注入视图（端到端）
- [ ] ttl=1 一次性消费与 ttl>1 累积注入两种语义有测试
- [ ] 压缩发生后事实仍经摘要 Current State 段保留
- [ ] 判定器从入参判定语义（非硬匹配工具名）的示例可用
