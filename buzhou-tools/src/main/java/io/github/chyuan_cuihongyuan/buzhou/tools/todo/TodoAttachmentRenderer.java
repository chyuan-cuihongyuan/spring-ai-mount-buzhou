package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;

import java.util.List;
import java.util.Optional;

/**
 * todo 清单的 Attachment 渲染器（spec 06 存储 Schema 节：变更按下轮注入前的 Attachment
 * 管道渲染进 prompt，复用 ticket 26 的 system-reminder 注入形态）。
 *
 * <p>桥接方式同 guard 的 FactAttachmentRenderer：memory 注入视图构建方持有可选引用。
 * 与事实渲染器并存时由装配侧组合（两个 {@link AttachmentRenderer} 文本拼接）。
 */
public class TodoAttachmentRenderer implements AttachmentRenderer {

    private final TodoStore store;

    public TodoAttachmentRenderer(TodoStore store) {
        this.store = store;
    }

    @Override
    public Optional<String> render(String sessionId, int currentTurn) {
        List<TodoItem> items = store.load(sessionId);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("## 任务清单（todo）\n" + TodoTool.render(items));
    }
}
