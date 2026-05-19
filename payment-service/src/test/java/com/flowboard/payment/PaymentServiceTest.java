package com.flowboard.payment;

import com.flowboard.payment.dto.PaymentOrderRequest;
import com.flowboard.payment.dto.PaymentVerifyRequest;
import com.flowboard.payment.entity.PaymentTransaction;
import com.flowboard.payment.entity.Subscription;
import com.flowboard.payment.entity.SubscriptionTier;
import com.flowboard.payment.repository.PaymentTransactionRepository;
import com.flowboard.payment.repository.SubscriptionRepository;
import com.flowboard.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.flowboard.payment.client.WorkspaceServiceClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private WorkspaceServiceClient workspaceServiceClient;

    // We use manual field-injection so we can set @Value properties
    // that Spring would normally inject from application.properties.
    private PaymentServiceImpl paymentService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private PaymentOrderRequest proOrderRequest;
    private PaymentOrderRequest enterpriseOrderRequest;
    private PaymentTransaction demoTransaction;

    @BeforeEach
    void setUp() throws Exception {
        paymentService = new PaymentServiceImpl();
        injectField("transactionRepository", transactionRepository);
        injectField("subscriptionRepository", subscriptionRepository);
        injectField("workspaceServiceClient", workspaceServiceClient);
        // Use dummy keys so Razorpay SDK always falls back to DEMO mode
        injectField("keyId", "rzp_test_DUMMY");
        injectField("keySecret", "dummy_secret");
        injectField("workspaceServiceUrl", "http://localhost:8082");

        // PRO order request
        proOrderRequest = new PaymentOrderRequest();
        proOrderRequest.setWorkspaceId(42L);
        proOrderRequest.setUserId(7L);
        proOrderRequest.setTargetTier(SubscriptionTier.PRO);

        // ENTERPRISE order request
        enterpriseOrderRequest = new PaymentOrderRequest();
        enterpriseOrderRequest.setWorkspaceId(42L);
        enterpriseOrderRequest.setUserId(7L);
        enterpriseOrderRequest.setTargetTier(SubscriptionTier.ENTERPRISE);

        // A pre-saved DEMO transaction (as returned from createOrder)
        demoTransaction = new PaymentTransaction();
        demoTransaction.setWorkspaceId(42L);
        demoTransaction.setUserId(7L);
        demoTransaction.setRazorpayOrderId("order_DEMO_1234567890");
        demoTransaction.setAmount(SubscriptionTier.PRO.getMonthlyPriceInr() * 100);
        demoTransaction.setCurrency("INR");
        demoTransaction.setTargetTier(SubscriptionTier.PRO);
        demoTransaction.setStatus("CREATED");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Reflectively injects a field by name (mirrors WorkspaceServiceTest style). */
    private void injectField(String fieldName, Object value) {
        try {
            var field = PaymentServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(paymentService, value);
        } catch (Exception e) {
            throw new RuntimeException("Field injection failed: " + fieldName, e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // createOrder tests
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PS-01: createOrder falls back to DEMO mode when Razorpay keys are invalid")
    void createOrder_demoFallback_whenRazorpayFails() {
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = paymentService.createOrder(proOrderRequest);

        assertNotNull(result);
        assertTrue(result.getRazorpayOrderId().startsWith("order_DEMO_"),
                "Expected a DEMO order ID prefix");
        assertEquals("CREATED", result.getStatus());
        assertEquals(42L, result.getWorkspaceId());
        assertEquals(SubscriptionTier.PRO, result.getTargetTier());
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("PS-02: createOrder calculates correct PRO amount (₹1500 → 150000 paise)")
    void createOrder_correctProAmount() {
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = paymentService.createOrder(proOrderRequest);

        // PRO = ₹1500/mo → 1500 * 100 = 150000 paise
        assertEquals(150_000, result.getAmount());
        assertEquals("INR", result.getCurrency());
    }

    @Test
    @DisplayName("PS-03: createOrder calculates correct ENTERPRISE amount (₹5000 → 500000 paise)")
    void createOrder_correctEnterpriseAmount() {
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = paymentService.createOrder(enterpriseOrderRequest);

        // ENTERPRISE = ₹5000/mo → 5000 * 100 = 500000 paise
        assertEquals(500_000, result.getAmount());
        assertEquals(SubscriptionTier.ENTERPRISE, result.getTargetTier());
    }

    // ════════════════════════════════════════════════════════════════════════
    // verifyPayment tests
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PS-04: verifyPayment (DEMO) marks transaction SUCCESS and saves subscription")
    void verifyPayment_demoOrder_success() {
        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setRazorpayOrderId("order_DEMO_1234567890");
        req.setRazorpayPaymentId("pay_DEMO_" + System.currentTimeMillis());
        req.setRazorpaySignature("demo_signature");

        when(transactionRepository.findByRazorpayOrderId("order_DEMO_1234567890"))
                .thenReturn(demoTransaction);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.findByWorkspaceId(42L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = paymentService.verifyPayment(req);

        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getRazorpayPaymentId());
        // Subscription must be created/updated
        verify(subscriptionRepository).save(any());
    }

    @Test
    @DisplayName("PS-05: verifyPayment (DEMO) upgrades subscription tier to PRO")
    void verifyPayment_demoOrder_setsTierToPro() {
        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setRazorpayOrderId("order_DEMO_1234567890");
        req.setRazorpayPaymentId("pay_DEMO_abc");
        req.setRazorpaySignature("demo_signature");

        when(transactionRepository.findByRazorpayOrderId(anyString())).thenReturn(demoTransaction);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.findByWorkspaceId(42L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            Subscription sub = inv.getArgument(0);
            assertEquals(SubscriptionTier.PRO, sub.getTier(), "Subscription tier should be PRO");
            return sub;
        });

        paymentService.verifyPayment(req);

        verify(subscriptionRepository).save(argThat(sub ->
                sub.getTier() == SubscriptionTier.PRO && sub.getWorkspaceId().equals(42L)
        ));
    }

    @Test
    @DisplayName("PS-06: verifyPayment throws RuntimeException when order ID is not found")
    void verifyPayment_orderNotFound_throwsException() {
        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setRazorpayOrderId("order_NONEXISTENT_999");

        when(transactionRepository.findByRazorpayOrderId("order_NONEXISTENT_999"))
                .thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.verifyPayment(req));
        assertTrue(ex.getMessage().contains("Order not found"),
                "Error message should mention 'Order not found'");
    }

    @Test
    @DisplayName("PS-07: verifyPayment (DEMO) calls workspace-service PUT to update tier")
    void verifyPayment_demoOrder_notifiesWorkspaceService() {
        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setRazorpayOrderId("order_DEMO_1234567890");
        req.setRazorpayPaymentId("pay_DEMO_xyz");
        req.setRazorpaySignature("demo_signature");

        when(transactionRepository.findByRazorpayOrderId(anyString())).thenReturn(demoTransaction);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.findByWorkspaceId(anyLong())).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.verifyPayment(req);

        // Workspace-service should receive a PUT call to upgrade the tier
        verify(workspaceServiceClient).updateWorkspaceTier(42L, "PRO");
    }

    @Test
    @DisplayName("PS-08: verifyPayment (DEMO) updates existing subscription when one already exists")
    void verifyPayment_demoOrder_updatesExistingSubscription() {
        Subscription existingSub = new Subscription();
        existingSub.setWorkspaceId(42L);
        existingSub.setTier(SubscriptionTier.FREE);
        existingSub.setActive(true);

        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setRazorpayOrderId("order_DEMO_1234567890");
        req.setRazorpayPaymentId("pay_DEMO_update");
        req.setRazorpaySignature("demo_signature");

        when(transactionRepository.findByRazorpayOrderId(anyString())).thenReturn(demoTransaction);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Existing subscription found
        when(subscriptionRepository.findByWorkspaceId(42L)).thenReturn(Optional.of(existingSub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.verifyPayment(req);

        // Verify existing subscription tier was updated (not a new one created)
        verify(subscriptionRepository).save(argThat(sub ->
                sub == existingSub && sub.getTier() == SubscriptionTier.PRO
        ));
    }

    // ════════════════════════════════════════════════════════════════════════
    // getSubscription tests
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PS-09: getSubscription returns existing subscription record")
    void getSubscription_returnsExisting() {
        Subscription sub = new Subscription();
        sub.setWorkspaceId(42L);
        sub.setTier(SubscriptionTier.PRO);
        sub.setActive(true);
        sub.setValidUntil(LocalDateTime.now().plusMonths(1));

        when(subscriptionRepository.findByWorkspaceId(42L)).thenReturn(Optional.of(sub));

        Subscription result = paymentService.getSubscription(42L);

        assertEquals(SubscriptionTier.PRO, result.getTier());
        assertEquals(42L, result.getWorkspaceId());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("PS-10: getSubscription returns FREE-tier default when no record exists")
    void getSubscription_defaultsFreeWhenNoRecord() {
        when(subscriptionRepository.findByWorkspaceId(99L)).thenReturn(Optional.empty());

        Subscription result = paymentService.getSubscription(99L);

        assertNotNull(result, "Should return a non-null default subscription");
        assertEquals(SubscriptionTier.FREE, result.getTier(),
                "Default tier should be FREE for a workspace with no subscription");
        assertEquals(99L, result.getWorkspaceId());
        assertTrue(result.isActive());
    }

    // ════════════════════════════════════════════════════════════════════════
    // getTransactionHistory tests
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PS-11: getTransactionHistory returns ordered list for a workspace")
    void getTransactionHistory_returnsOrdered() {
        PaymentTransaction t1 = new PaymentTransaction();
        t1.setWorkspaceId(42L);
        t1.setStatus("SUCCESS");
        t1.setTargetTier(SubscriptionTier.PRO);

        PaymentTransaction t2 = new PaymentTransaction();
        t2.setWorkspaceId(42L);
        t2.setStatus("FAILED");
        t2.setTargetTier(SubscriptionTier.PRO);

        when(transactionRepository.findByWorkspaceIdOrderByCreatedAtDesc(42L))
                .thenReturn(List.of(t1, t2));

        List<PaymentTransaction> history = paymentService.getTransactionHistory(42L);

        assertEquals(2, history.size());
        assertEquals("SUCCESS", history.get(0).getStatus());
        assertEquals("FAILED", history.get(1).getStatus());
        verify(transactionRepository).findByWorkspaceIdOrderByCreatedAtDesc(42L);
    }

    @Test
    @DisplayName("PS-12: getTransactionHistory returns empty list when workspace has no transactions")
    void getTransactionHistory_emptyForNewWorkspace() {
        when(transactionRepository.findByWorkspaceIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of());

        List<PaymentTransaction> history = paymentService.getTransactionHistory(100L);

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
}
