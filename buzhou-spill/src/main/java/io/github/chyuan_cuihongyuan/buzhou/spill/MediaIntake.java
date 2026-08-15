package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.session.MediaRef;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 媒体字节摄取助手（spec 35 §C / T120 / impl-95）：应用拿到的常是**字节**（上传/剪贴板），
 * {@link MediaRef} 又是 URI-only（防 base64 塞消息体）——本助手补「字节 → spill 落盘 →
 * URI 引用」闭环：落盘沿用 spill 全部语义（配额、原子写、sha256 meta、随会话级联删）。
 *
 * <p><b>生命周期</b>：证据属主 = sessionId（会话删除级联清理）；fork 引用计数语义兼容
 * （spec 26——fork 时 acquireSessionReferences 登记引用）。
 */
public final class MediaIntake {

    private final SpillStore store;
    private final int previewChars;

    public MediaIntake(SpillStore store) {
        this(store, 2048);
    }

    public MediaIntake(SpillStore store, int previewChars) {
        this.store = store;
        this.previewChars = previewChars;
    }

    /**
     * 摄取字节为可引用媒体：落 spill（UTF-8 无损字节→文本通道——二进制安全经
     * ISO-8859-1 保真往返，读回经 {@link #readBack} 还原原字节）。
     *
     * @param bytes     媒体字节（非空）
     * @param mimeType  MIME 类型（进 MediaRef；内容通道统一 text/plain 保真）
     * @param agentName 属主 agent（目录成分）
     * @param sessionId 属主会话（目录成分 + 级联删除锚）
     * @return 可直接传入 {@code chat(input, media)} 的 MediaRef（spill:// URI）
     */
    public MediaRef intake(byte[] bytes, String mimeType, String agentName, String sessionId) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("intake 字节不可为空");
        }
        // 二进制保真：按 Latin-1 逐字节映射为 char（0-255 双向无损），读回同口径还原
        StringBuilder preserved = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            preserved.append((char) (b & 0xFF));
        }
        SpillUri uri = new SpillUri(SpillModule.sanitizeComponent(agentName),
                SpillModule.sanitizeComponent(sessionId),
                "media-" + UUID.randomUUID().toString().substring(0, 8));
        store.store(new SpillEntry(uri, preserved.toString(), "application/octet-stream",
                preserved.length(), java.time.Instant.now()), previewChars);
        return new MediaRef(mimeType, java.net.URI.create(uri.toString()));
    }

    /** 回读还原原字节（Latin-1 双向无损）。 */
    public byte[] readBack(MediaRef ref) {
        String content = store.load(SpillUri.parse(ref.uri().toString()))
                .orElseThrow(() -> new IllegalArgumentException("媒体证据已被清理（" + ref.uri() + "）"));
        byte[] bytes = new byte[content.length()];
        for (int i = 0; i < content.length(); i++) {
            bytes[i] = (byte) content.charAt(i);
        }
        return bytes;
    }

    /** 文本媒体便捷摄取（UTF-8；读回用 {@link #readBackText}）。 */
    public MediaRef intakeText(String text, String mimeType, String agentName, String sessionId) {
        return intake(text.getBytes(StandardCharsets.UTF_8), mimeType, agentName, sessionId);
    }

    /** 文本媒体回读（UTF-8）。 */
    public String readBackText(MediaRef ref) {
        return new String(readBack(ref), StandardCharsets.UTF_8);
    }
}
