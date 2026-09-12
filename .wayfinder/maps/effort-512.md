# Wayfinder Map — Buzhou 提示词模板严格渲染（effort #512，E 会话第 13 轮）

> E 会话第 13 轮（401 提示词注册表扩散轮）。勘察：注册表管版本/标签/
> 使用统计——**模板正文**只是一段文本：宿主自己 {{var}} 替换，变量漏传
> 时 `{{name}}` **原样漏进 prompt**（静默失败——模型看到占位符语法，
> 生产常见坑）。Jinja2 StrictUndefined 思想：未定义变量即抛错并列全。

## Destination

`core.prompt.PromptTemplate`（静态原语）：`variables(template)` 抽取
`{{name}}` 占位符（去重有序）；`render(template, vars)` 严格渲染——
缺失变量即抛 IllegalStateException **一次列全缺失名单**（StrictUndefined
语义），多余变量忽略，`{{name` 未闭合语法错带位置；`validate(template,
provided)` → ValidationResult(ok, missing) 预检（装配期/评测期零成本
体检）。与 401 注册表组合：resolve 后 render（宿主一行）。

## Notes

- 号段：spec 512 / T775–T776 / impl-415。
- 借鉴源：Jinja2 StrictUndefined（未定义即抛+列全）；Langfuse 模板
  变量面。
- 诚实边界：只做单层变量替换（不做过滤器/循环/条件——提示词不是通用
  模板引擎）；非占位符文本原样保留。

## Out of scope

- 过滤器/循环/条件语法；嵌套变量；注册表内自动渲染（宿主显式组合）。

## Tickets

- [x] [T775 变量抽取与严格渲染](../tickets/T775-prompt-template-render.md)
- [x] [T776 预检面](../tickets/T776-prompt-template-validate.md)
