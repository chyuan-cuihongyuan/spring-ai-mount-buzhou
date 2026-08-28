package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

/** PII 类型（spec 86 §A / T329，Presidio 实体类型面的小子集）。 */
public enum PiiType {

    EMAIL("电子邮箱"),
    CN_PHONE("手机号"),
    CN_RESIDENT_ID("身份证号"),
    BANK_CARD("银行卡号"),
    IPV4("IPv4 地址");

    private final String label;

    PiiType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
