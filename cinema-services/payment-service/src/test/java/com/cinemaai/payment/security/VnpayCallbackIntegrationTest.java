package com.cinemaai.payment.security;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.controller.PaymentController;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.service.LoyaltyService;
import com.cinemaai.payment.service.OutboxPublisherWorker;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.cinemaai.payment.util.VNPayUtil;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, PaymentServiceImpl.class})
@TestPropertySource(properties = {"APPLICATION_NAME=security-test", "server.address=127.0.0.1", "server.port=0", "app.gateway.secret=gateway-test", "app.internal.secret=internal-test", "app.jwt.secret=01234567890123456789012345678901", "vnpay.hash-secret=test-secret", "vnpay.tmn-code=test", "vnpay.pay-url=https://example.test/pay", "vnpay.return-url=https://example.test/return", "vnpay.success-redirect-url=https://client.example.test/result"})
class VnpayCallbackIntegrationTest {
 @Autowired MockMvc mvc;
 @MockBean PaymentRepository payments;
 @MockBean OutboxEventRepository outbox;
 @MockBean OutboxPublisherWorker worker;
 @MockBean BookingClient booking;
 @MockBean LoyaltyService loyalty;
 @MockBean PaymentMapper mapper;

 MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder request, Map<String,String> fields) {
  fields.forEach(request::param);
  return request.param("vnp_SecureHash", VNPayUtil.hashAllFields(fields, "test-secret"));
 }
 @Test void invalidAndMissingSignatureNeverMutateOrContactBooking() throws Exception {
  mvc.perform(get("/api/v1/payments/vnpay/ipn").param("vnp_TxnRef", "42").param("vnp_SecureHash", "bad"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("97"));
  mvc.perform(post("/api/v1/payments/vnpay/ipn").param("vnp_TxnRef", "42"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("97"));
  verifyNoInteractions(payments, outbox, worker, booking, loyalty);
 }
 @Test void validSignatureReachesRealServiceAndReturnsMissingOrder() throws Exception {
  when(payments.findById(42L)).thenReturn(Optional.empty());
  var fields = Map.of("vnp_TxnRef", "42", "vnp_Amount", "10000");
  mvc.perform(signed(get("/api/v1/payments/vnpay/ipn"), fields)).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("01"));
  mvc.perform(signed(post("/api/v1/payments/vnpay/ipn"), fields)).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("01"));
  verify(payments, times(2)).findById(42L); verifyNoInteractions(outbox, booking, loyalty);
 }
 @Test void malformedAndMissingReferencesAreRejectedAfterSignatureVerification() throws Exception {
  mvc.perform(signed(get("/api/v1/payments/vnpay/ipn"), Map.of("vnp_Amount", "10000"))).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("01"));
  mvc.perform(signed(get("/api/v1/payments/vnpay/ipn"), Map.of("vnp_TxnRef", "not-a-number"))).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("01"));
  verifyNoInteractions(payments, outbox, booking, loyalty);
 }
 @Test void nonnumericAmountOnExistingPaymentDoesNotProduceSuccess() throws Exception {
  var payment = Payment.builder().id(42L).amount(new BigDecimal("100")).status(PaymentStatus.PENDING).build();
  when(payments.findById(42L)).thenReturn(Optional.of(payment));
  var fields = Map.of("vnp_TxnRef", "42", "vnp_Amount", "invalid", "vnp_ResponseCode", "00");
  mvc.perform(signed(get("/api/v1/payments/vnpay/ipn"), fields)).andExpect(status().isInternalServerError());
  org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.PENDING, payment.getStatus());
  verify(payments, never()).save(any()); verifyNoInteractions(outbox, booking, loyalty);
 }
 @Test void returnAndNullUseConfiguredRedirectAndPreserveQuery() throws Exception {
  for (String path : List.of("/api/v1/payments/vnpay/return", "/api/v1/payments/vnpay/null"))
   mvc.perform(get(java.net.URI.create(path + "?vnp_TxnRef=42&next=https%3A%2F%2Fevil.example"))).andExpect(status().isFound())
    .andExpect(header().string("Location", "https://client.example.test/result?vnp_TxnRef=42&next=https%3A%2F%2Fevil.example"));
  verifyNoInteractions(payments, outbox, booking, loyalty);
 }
 @Test void alreadySuccessfulPaymentRetryDoesNotWriteAnotherPaymentOrOutbox() throws Exception {
  var payment = Payment.builder().id(42L).amount(new BigDecimal("100")).status(PaymentStatus.SUCCESS).build();
  when(payments.findById(42L)).thenReturn(Optional.of(payment));
  var fields = Map.of("vnp_TxnRef", "42", "vnp_Amount", "10000", "vnp_ResponseCode", "00");
  for(int i=0;i<2;i++) mvc.perform(signed(get("/api/v1/payments/vnpay/ipn"), fields)).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("00"));
  verify(payments, never()).save(any()); verifyNoInteractions(outbox, booking, loyalty);
 }
}
