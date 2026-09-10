package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.service.VNPayService;
import com.sba301.cinemaai.config.VNPayConfig;
import com.sba301.cinemaai.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;

    public String buildPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String clientIp) {
        return buildPaymentUrl(txnRef, amount, orderInfo, clientIp, LocalDateTime.now().plusMinutes(15));
    }

    public String buildPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String clientIp,
                                  LocalDateTime expiresAt) {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString());
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", resolveIp(clientIp));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnpParams.put("vnp_CreateDate", formatter.format(calendar.getTime()));
        Date expiry = Date.from(expiresAt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        vnpParams.put("vnp_ExpireDate", formatter.format(expiry));

        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnPayConfig.getHashSecret());
        String queryString = buildEncodedQueryString(vnpParams);

        log.info("VNPay payment created for txnRef={} amount={}", txnRef, amount);
        return vnPayConfig.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifySignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null) return false;

        Map<String, String> filtered = new TreeMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");

        String expected = VNPayUtil.hashAllFields(filtered, vnPayConfig.getHashSecret());
        boolean valid = expected.equalsIgnoreCase(received);
        if (!valid) log.warn("VNPay signature verification failed");
        return valid;
    }

    private String buildEncodedQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });
        return sb.toString();
    }

    private String resolveIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank() || clientIp.contains(":")) {
            String defaultClientIp = vnPayConfig.getDefaultClientIp();
            return (defaultClientIp == null || defaultClientIp.isBlank()) ? "127.0.0.1" : defaultClientIp;
        }
        return clientIp;
    }
}
