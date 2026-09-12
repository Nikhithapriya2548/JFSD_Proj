package com.omnishop.notification.listener;

import com.omnishop.notification.model.Notification;
import com.omnishop.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock NotificationRepository repository;
    @InjectMocks NotificationListener listener;

    private Notification capture(Map<String, Object> event) {
        when(repository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        listener.onEvent(event);
        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(cap.capture());
        return cap.getValue();
    }

    @Test
    void orderCreatedRendersAndSaves() {
        Map<String, Object> e = new HashMap<>(Map.of(
                "eventType", "order.created", "orderId", 5,
                "userId", 7, "totalAmount", "199.99"));

        Notification n = capture(e);

        assertThat(n.getUserId()).isEqualTo(7L);
        assertThat(n.getRefId()).isEqualTo(5L);
        assertThat(n.getMessage()).contains("#5").contains("199.99");
    }

    @Test
    void paymentFailedKeepsOrderOnHold() {
        Map<String, Object> e = new HashMap<>(Map.of(
                "eventType", "payment.failed", "orderId", 9, "userId", 7));

        Notification n = capture(e);

        assertThat(n.getMessage()).contains("#9").contains("retry");
    }

    @Test
    void stockLowUsesProductRef() {
        Map<String, Object> e = new HashMap<>(Map.of(
                "eventType", "stock.low", "productId", 3, "name", "Widget",
                "stockQuantity", 2, "userId", 1));

        Notification n = capture(e);

        assertThat(n.getRefId()).isEqualTo(3L);
        assertThat(n.getMessage()).contains("Widget").contains("2 left");
    }

    @Test
    void unknownTypeFallsBack() {
        Map<String, Object> e = new HashMap<>(Map.of(
                "eventType", "something.new", "userId", 1));

        Notification n = capture(e);

        assertThat(n.getMessage()).startsWith("Event received:");
        assertThat(n.getRefId()).isNull();
    }

    @Test
    void stringIdsCoerced() {
        Map<String, Object> e = new HashMap<>(Map.of(
                "eventType", "payment.completed", "orderId", "12",
                "userId", "7", "amount", "50.00", "transactionId", "MOCK-TXN-X"));

        Notification n = capture(e);

        assertThat(n.getUserId()).isEqualTo(7L);
        assertThat(n.getRefId()).isEqualTo(12L);
        assertThat(n.getMessage()).contains("MOCK-TXN-X");
    }
}
