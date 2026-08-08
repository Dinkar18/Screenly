package com.dipu.MovieTicketBookingSystem.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {

    // Spring will automatically inject all beans implementing PaymentProvider.
    // The keys in the map will be the bean names (e.g., "stripe", "upi").
    private final Map<String, PaymentProvider> providers;

    /**
     * Gets the payment provider by name.
     *
     * @param paymentMethod The name of the payment method (e.g., "stripe", "upi")
     * @return The corresponding PaymentProvider
     * @throws IllegalArgumentException if the provider is not found
     */
    public PaymentProvider getProvider(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            paymentMethod = "stripe"; // Default provider
        }
        
        PaymentProvider provider = providers.get(paymentMethod.toLowerCase());
        
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod);
        }
        
        return provider;
    }
}
