package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.crypto.EnvelopeCipher;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;

/**
 * 加密会话导出容器（spec 510 / T771——333 信封加密通道扩散到 28 导出面；
 * age/OCI 加密 artifact 形态：密文容器 + 版本标记头）。
 *
 * <p>AAD 绑定<b>用途域</b>常量 {@code buzhou.session-export}——消息存储
 * 信封（333，AAD=消息标识）与导出封缄跨域剪贴互不可解（AAD 绑定语义
 * 延续）。单密钥环进程内口径（333 同）；双钥轮换由 EnvelopeCipher 原生
 * 支持。spill 证据引用不内嵌（28 同注记——密文化不改变引用语义）。
 */
public final class EncryptedSessionExport {

    /** 封缄标记头（版本化）。 */
    public static final String SEAL_PREFIX = "buzhou:session-export:v1:";

    /** 用途域 AAD（跨域剪贴防——333 AAD 绑定语义）。 */
    static final String PURPOSE_AAD = "buzhou.session-export";

    private final EnvelopeCipher cipher;

    public EncryptedSessionExport(EnvelopeCipher cipher) {
        if (cipher == null) {
            throw new IllegalArgumentException("EnvelopeCipher 必须非空");
        }
        this.cipher = cipher;
    }

    /** 导出 → 封缄密文容器（标记头 + 信封密文）。 */
    public String seal(SessionExport export) {
        if (export == null) {
            throw new IllegalArgumentException("SessionExport 必须非空");
        }
        return SEAL_PREFIX + cipher.encrypt(export.toJson(), PURPOSE_AAD);
    }

    /** 封缄容器 → 导出（标记/密钥/AAD/JSON 任一不符 → DATA_CORRUPTION 带修法）。 */
    public SessionExport open(String sealed) {
        if (!isSealed(sealed)) {
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION,
                    "非加密会话导出封缄（缺少标记头 " + SEAL_PREFIX + "）——"
                            + "请用 EncryptedSessionExport.seal 生成的封缄，或改用"
                            + " SessionExport.fromJson 直接导入明文导出");
        }
        String envelope = sealed.substring(SEAL_PREFIX.length());
        String json;
        try {
            json = cipher.decrypt(envelope, PURPOSE_AAD);
        } catch (BuzhouException e) {
            throw e;
        } catch (RuntimeException e) {
            throw decryptionFailed(e);
        }
        try {
            return SessionExport.fromJson(json);
        } catch (RuntimeException e) {
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION,
                    "解密成功但载荷非合法会话导出 JSON（密钥对、内容被换）", e);
        }
    }

    /** 封缄形态判定（宿主路由明文/密文两导出形态）。 */
    public static boolean isSealed(String value) {
        return value != null && value.startsWith(SEAL_PREFIX);
    }

    private static BuzhouException decryptionFailed(RuntimeException cause) {
        return new BuzhouException(ErrorCode.DATA_CORRUPTION,
                "导出封缄解密失败（主密钥不符或密文被篡改）——"
                        + "请确认使用与 seal 相同的密钥环（含轮换旧钥）", cause);
    }
}
