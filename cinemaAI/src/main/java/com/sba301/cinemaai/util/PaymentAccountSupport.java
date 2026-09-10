package com.sba301.cinemaai.util;

import com.sba301.cinemaai.enums.PaymentProvider;
import java.util.Map;

public final class PaymentAccountSupport {

    private PaymentAccountSupport() {
    }

    public static String resolveFromCallback(PaymentProvider provider, Map<String, String> params) {
        if (provider == PaymentProvider.MOCK) {
            return "MOCK";
        }
        if (params == null || params.isEmpty()) {
            return null;
        }
        String bankCode = params.get("vnp_BankCode");
        if (bankCode == null || bankCode.isBlank()) {
            return null;
        }
        return resolveBankLabel(bankCode.trim());
    }

    public static String resolveBankLabel(String bankCode) {
        return switch (bankCode) {
            case "NCB" -> "Ngân hàng NCB";
            case "VIETCOMBANK", "VCB" -> "Vietcombank";
            case "VIETINBANK", "ICB" -> "VietinBank";
            case "BIDV" -> "BIDV";
            case "AGRIBANK", "AGR" -> "Agribank";
            case "TECHCOMBANK", "TCB" -> "Techcombank";
            case "VPB", "VPBANK" -> "VPBank";
            case "MB", "MBBANK" -> "MB Bank";
            case "ACB" -> "ACB";
            case "TPB", "TPBANK" -> "TPBank";
            case "VNPAYQR" -> "VNPAY QR";
            default -> bankCode;
        };
    }
}
