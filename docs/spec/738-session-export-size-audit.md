# 738 — 导出体积去向审计

> 来源：G 会话第 39 轮 = effort #738（存储成本归因思想）/ [T1076](../../.wayfinder/tickets/T1076-session-export-size.md) / [T1077](../../.wayfinder/tickets/T1077-session-export-size-verify.md) / impl 638。
> **补档注记**：本 spec 文件在 R39 轮漏写——收口时补档。

## Problem

SessionExport（messages/summary/state/extensions 四段）导出体积无归因面——「导出为什么这么大」要肉眼拆 JSON。

## Solution

`SessionExportSizeAudit.analyze(SessionExport)` 纯函数：按段字符归因 Segment(segment, chars, share)——messages（content 长度和）/summary（存在性小头标记）/state（键+值长度）/ext:*（扩展段按名分行），chars 降序+同数名字典序稳定+占比守恒（sum=1.0 可断言）。诚实边界：字符估算口径（精确序列化大小可 toJson 交叉验证）。

## Testing Decisions

四段归因+占比守恒+最大段定位；空导出零；null fail-fast。

## Out of Scope

精确序列化字节口径（估算先行）；自动瘦身动作。
