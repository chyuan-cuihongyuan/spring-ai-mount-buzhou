---
name: file-editing
description: 文件编辑深度指引：副本分离流程（copy_file → str_replace）、写侧长内容参数与沙箱边界
allowed-tools: read_file, write_file, copy_file, str_replace, read_range
---

# 文件编辑指引

## 副本分离（先 copy 后编辑）

只读来源（快照区/Spill 落盘文件）不可直改——编辑类工具直改只读区会被 Hook 拦截。
正确流程：

1. `copy_file(srcPath=只读来源, destPath=工作区路径)` 建工作副本；
2. `str_replace(path=副本路径, oldStr=..., newStr=...)` 编辑副本；
3. 如需回写只读来源之外的正式位置，用 `write_file` 写最终产物。

## str_replace 使用要点

- `oldStr` 必须在文件中**唯一出现**，否则失败——失败时把 `oldStr` 扩大为含更多上下文的片段重试。
- 长替换内容不要直接拼进 `newStr`（长入参有自截断风险）：先 `write_file` 写到临时文件，
  再用 `newStrPath=临时文件路径` 让框架自动加载全文。

## write_file 长内容

- `content` 与 `contentPath` 二选一；`contentPath` 非空时框架加载该文件全文覆盖 `content`。
- `contentPath` 加载失败会**阻断**本次写入（写侧失败语义非对称，杜绝残缺产物外流）——
  修正路径后重发即可。

## 沙箱边界

- 所有路径限沙箱 root（默认应用工作目录）内；`..` 逃逸、符号链接逃逸一律拒绝。
- 越界报错时改用沙箱内路径，或确认目标在 `allowed-paths` 白名单内。

## 超长文件

- `read_file` 整读超阈值会自动落盘，返回预览 + 回读指针；按指针用 `read_range` 分段续读，
  不要反复整读同一长文件。
