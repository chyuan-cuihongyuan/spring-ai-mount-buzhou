package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import java.util.List;

/**
 * 确认模型（spec 07 确认事件 schema）：通用 = yes/no + 多选项 + 单文本输入 + hint 嵌 diff。
 *
 * @param title   确认标题
 * @param options 选项列表（含 hasInput 标记单输入控件）
 */
public record Confirmation(String title, List<ConfirmOption> options) {

    public Confirmation {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
