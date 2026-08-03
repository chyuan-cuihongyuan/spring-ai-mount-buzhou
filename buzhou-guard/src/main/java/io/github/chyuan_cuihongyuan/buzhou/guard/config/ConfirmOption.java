package io.github.chyuan_cuihongyuan.buzhou.guard.config;

/**
 * 确认选项（spec 07 确认事件 schema）。
 *
 * @param id               选项 id（如 approve/approval/reject）
 * @param label            展示文案
 * @param value            选择值（与 id 通常一致）
 * @param hasInput         是否带单文本输入控件
 * @param inputPlaceholder 输入框占位文案（hasInput=true 时）
 * @param inputType        输入类型（text/number 等，hasInput=true 时）
 */
public record ConfirmOption(
        String id,
        String label,
        String value,
        boolean hasInput,
        String inputPlaceholder,
        String inputType) {

    public ConfirmOption {
        id = id == null ? "" : id;
        label = label == null ? "" : label;
        value = value == null ? id : value;
        inputPlaceholder = inputPlaceholder == null ? "" : inputPlaceholder;
        inputType = inputType == null ? "text" : inputType;
    }

    /** 便捷构造：无输入控件。 */
    public ConfirmOption(String id, String label, String value) {
        this(id, label, value, false, "", "text");
    }
}
