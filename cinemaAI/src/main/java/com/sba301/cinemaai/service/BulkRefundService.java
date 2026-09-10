package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.response.refund.BulkRefundResponse;
import com.sba301.cinemaai.entity.Showtime;

public interface BulkRefundService {

    BulkRefundResponse processBulkRefund(Showtime showtime, String reason);
}
