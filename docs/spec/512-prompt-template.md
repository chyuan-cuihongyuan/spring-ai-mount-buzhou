# Spec 512 — 提示词模板严格渲染（effort #512）

> wayfinder map：`.wayfinder/maps/effort-512.md`（T775–T776）。E 会话第 13 轮。

## Problem Statement

401 注册表管版本/标签/使用统计，模板正文只是文本——宿主自行 {{var}}
替换时变量漏传会把 `{{name}}` **原样漏进 prompt**（静默失败，生产常见
坑）。Jinja2 StrictUndefined：未定义变量即抛错并列全缺失。

## Solution

`core.prompt.PromptTemplate`（静态原语，401 注册表组合消费）：

- `variables(template)`：抽取 `{{name}}`（name=[A-Za-z_][A-Za-z0-9_]*，
  容忍两侧空白；去重保序）。
- `render(template, vars)`：严格渲染——缺失变量一次列全
  （IllegalStateException，StrictUndefined 语义）；多余变量忽略；
  `{{name` 未闭合 → 语法错带位置。
- `validate(template, provided)` → `ValidationResult(boolean ok,
  List<String> missing)` 预检面（装配期/评测期零成本体检）。
- 与注册表组合：`render(registry.resolve(name).body(), vars)` 宿主一行。

## User Stories

1. 作为宿主，我想变量漏传时启动/评测期就报错并列全缺失， so 不把
   `{{name}}` 占位符语法发给模型（静默劣化）。
2. 作为评测方，我想预检数据集行与模板的变量匹配， so run 前脏数据
   零 token 出局（134 期望门同哲学）。

## Implementation Decisions

- 单层变量替换——不做过滤器/循环/条件（提示词不是通用模板引擎，
  诚实边界）。
- 渲染缺失=异常、预检缺失=返回值：两个通道分别对应「运行时 fail-fast」
  与「批前体检」。

## Testing Decisions

- 抽取：多变量/空白容忍/去重保序/非占位符原样。
- 渲染：缺失一次列全（含全部缺失名）、多余忽略、未闭合语法错带位置。
- 预检：ok/missing 两态；与注册表 publish/resolve 组合用例。

## Out of Scope

- 过滤器/循环/条件；嵌套变量；注册表内自动渲染。

## Further Notes

- 新公共类型 `PromptTemplate`（嵌套 `ValidationResult`）随轮 regenerate
  快照 + api-surface.md 加行。
