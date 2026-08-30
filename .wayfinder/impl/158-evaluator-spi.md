# 158 — 评估器 SPI 与内置三评估器

**Parent:** spec 52 §C / [T192](../tickets/T192-evaluator-spi.md)

**Status:** done

- [x] Evaluator SPI（evaluate(actual, expected, item) → EvalScore）+ EvalScore（detail 512 截断）
- [x] 内置三评估器：EXACT（trim 全等）/ CONTAINS（子串）/ REGEX（expected 为正则，find 语义；
  非法正则构造期 fail-fast 带修法）
- [x] 依赖勘察：jayway json-path 不在依赖树 → JSON_PATH 不做（零新依赖纪律；入档）
- [x] LLM-as-judge：SPI 即口，不内置不做门禁（边界沿用 effort #7）
- [x] 5 测试绿（含自定义 Evaluator 直通）
