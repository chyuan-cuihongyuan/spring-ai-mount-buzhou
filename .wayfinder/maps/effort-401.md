# Wayfinder Map — Buzhou 提示词注册表（effort #401，D 会话第 2 轮）

> D 会话第 2 轮。勘察（2026-09-08）：core 全量 grep `prompt` 仅命中
> PromptPrefixCache（供应商侧前缀缓存）与 eval/hook 内的提示词常量——
> **无任何提示词版本化管理**。提示词改一个字是 Agent 行为变更的最大
> 单点，但当前宿主只能改代码重发：无版本、无标签、无回滚、无「这条
> 轮次用了哪版」的事实。

## Destination

`core.prompt` 包（Langfuse prompt management 借鉴——版本 + 标签双轴）：
`PromptVersion`（不可变快照：name/version/body/note/publishedAt）+
`PromptRegistry`（publish 单调版本 + `latest` 自动指针；`label(name,label,version)`
标签重指=晋级/回滚；resolve by label / by version 钉版）+
`InMemoryPromptRegistry`（进程内实现，per-name 同步）。装配：
`buzhou.prompt.templates[{name,body,label}]` yml 播种（同 body 幂等跳过——
重启不掀版本；声明 label 自动指向最新）+ bean 恒在（空注册表零行为变化）。

## Notes

- 号段：spec 401 / T693–T694 / impl-374。
- 借鉴源：Langfuse（28k★）Prompt Management——版本号单调递增、
  label（production/staging）指针可移动、按版本钉取。
- 纪律：标签语义归宿主（registry 只管指针不解释标签名）；publish 不
  触发事件流（观测扩散轮候选）；body 相等即幂等是 yml 播种的诚实口径。

## Out of scope

- DB/Redis 持久注册表（进程内先行——跨实例共享真需求出现再议）；
- publish/label 事件流与面板段；模板变量渲染（Spring AI StTemplate
  宿主侧组合即可，registry 不越界）；A/B 流量分配（eval 族已有 A/B 跑批）。

## Tickets

- [x] [T693 PromptRegistry 版本语义](../tickets/T693-prompt-registry.md)
- [x] [T694 yml 播种装配](../tickets/T694-prompt-yml-seeding.md)
