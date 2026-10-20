package com.cinemaai.catalog.enums;

public enum ReviewReportReason {
    SPAM("Spam / Quảng cáo"),
    INAPPROPRIATE("Nội dung không phù hợp"),
    SPOILER("Tiết lộ nội dung phim (Spoiler)"),
    HARASSMENT("Quấy rối / Công kích"),
    OFF_TOPIC("Không liên quan đến phim"),
    OTHER("Khác");

    private final String descriptionVi;

    ReviewReportReason(String descriptionVi) {
        this.descriptionVi = descriptionVi;
    }

    public String getDescriptionVi() {
        return descriptionVi;
    }
}
