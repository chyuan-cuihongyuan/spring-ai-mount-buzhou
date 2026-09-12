# Wayfinder Map — Buzhou 数据集 CSV 互操作（effort #527，E 会话第 28 轮）

> E 会话第 28 轮。勘察：数据集只经 API 逐项添加/轨迹回流——与表格工具
> （Excel/Sheets/HF datasets）的 CSV 双向搬运空白。LangSmith/HF datasets
> CSV 形态思想。

## Destination

`eval.EvalDatasetCsv`（纯函数）：toCsv（RFC 4180 转义——逗号/引号/换行
字段自动包裹）+ fromCsv（状态机解析、表头宽松校验 input,expected、空行
容忍、候选 id 空）+ export(Writer) 行数返回。

## Notes

- 号段：spec 527 / T807–808 / impl-430。
- 借鉴源：LangSmith / HuggingFace datasets CSV 形态。

## Out of scope

- 溯源/元数据列；流式大文件；分号制表符方言。

## Tickets

- [x] [T807 RFC4180 往返](../tickets/T807-csv-roundtrip.md)
- [x] [T808 表头校验与导出](../tickets/T808-csv-header-export.md)
