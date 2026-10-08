package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.quote.CheckoutQuoteRequest;
import com.cinemaai.catalog.dto.response.quote.CheckoutQuoteResponse;

public interface CheckoutQuoteService {
    CheckoutQuoteResponse quote(CheckoutQuoteRequest request);
}
