package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.catalog.CheckoutQuoteRequest;
import com.sba301.cinemaai.dto.response.catalog.CheckoutQuoteResponse;

public interface CatalogCheckoutService {

    CheckoutQuoteResponse getCheckoutQuote(String email, CheckoutQuoteRequest request);
}
