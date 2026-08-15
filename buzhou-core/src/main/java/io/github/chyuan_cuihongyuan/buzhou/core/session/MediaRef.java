package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.net.URI;
import java.util.Map;

/**
 * 多模态输入媒体引用（spec 27 / T106 / impl-81）：{@code chat(input, media)} 携带的
 * 图片等媒体，URI 引用形态（模型侧按 URI 拉取）——字节直传不入 API（避免 base64
 * 塞消息体膨胀存储；字节由应用侧落对象存储/spill 后以 URI 引用）。
 *
 * <p><b>重发策略</b>（内存视图口径，spec 27）：媒体仅随<b>最近一条</b>带媒体的用户消息
 * 重发；更早轮次的带媒体消息在视图中降级为文本标记（媒体留在 store 全量保留，
 * token 成本按每媒体固定 320 计入预算估算）。
 *
 * @param mimeType MIME 类型（如 {@code image/png}、{@code application/pdf}）
 * @param uri      媒体地址（https/data 等模型可达 URI）
 *
 * @since 1.0.0
 */
public record MediaRef(String mimeType, URI uri) {

    /** 每媒体固定 token 计数（估算口径：尺寸未知，按中位图片档位计——spec 27）。 */
    public static final int TOKENS_PER_MEDIA = 320;

    public MediaRef {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MediaRef.mimeType 不可为空（如 image/png）");
        }
        if (uri == null) {
            throw new IllegalArgumentException("MediaRef.uri 不可为 null");
        }
    }

    public static MediaRef of(String mimeType, String uri) {
        return new MediaRef(mimeType, URI.create(uri));
    }

    /** metadata 序列化形态（BuzhouMessage.metadata["mediaRefs"] 列表项；store JSON 持久化）。 */
    public Map<String, String> toMetadata() {
        return Map.of("mimeType", mimeType, "uri", uri.toString());
    }

    /** 从 metadata 反解（损坏项跳过返回 null）。 */
    public static MediaRef fromMetadata(Object item) {
        if (item instanceof Map<?, ?> map
                && map.get("mimeType") instanceof String mime
                && map.get("uri") instanceof String uriStr) {
            try {
                return new MediaRef(mime, URI.create(uriStr));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
