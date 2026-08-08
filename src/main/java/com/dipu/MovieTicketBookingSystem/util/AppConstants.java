package com.dipu.MovieTicketBookingSystem.util;

public final class AppConstants {

    private AppConstants() {
        // Restrict instantiation
    }

    // Cookie Constants
    public static final int COOKIE_MAX_AGE_7_DAYS = 7 * 24 * 60 * 60;
    public static final String COOKIE_NAME_TOKEN = "token";

    // Stripe Constants
    public static final String EVENT_PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";

    // MDC Constants
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";
    public static final String MDC_CLIENT_IP_KEY = "clientIp";

    // Auth & OTP Constants
    public static final int OTP_EXPIRY_MINUTES = 10;
    public static final String OTP_EMAIL_SUBJECT = "Verify Your CineReserve Account";
    public static final String RESET_EMAIL_SUBJECT = "Password Reset Request";

    // Showtime Constants
    public static final int CLEANING_BUFFER_MINUTES = 30;
}
