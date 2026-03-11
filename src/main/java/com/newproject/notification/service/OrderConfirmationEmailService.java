package com.newproject.notification.service;

import com.newproject.notification.dto.OrderConfirmationItem;
import com.newproject.notification.dto.OrderConfirmationRequest;
import com.newproject.notification.dto.SmtpRuntimeSettings;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderConfirmationEmailService {
    private static final Logger logger = LoggerFactory.getLogger(OrderConfirmationEmailService.class);

    private final MessageSource messageSource;
    private final SmtpSettingsClient smtpSettingsClient;
    private final boolean fallbackMailEnabled;
    private final String fallbackFromAddress;
    private final String fallbackHost;
    private final Integer fallbackPort;
    private final String fallbackUsername;
    private final String fallbackPassword;
    private final boolean fallbackSmtpAuth;
    private final boolean fallbackSmtpStarttls;

    public OrderConfirmationEmailService(
        MessageSource messageSource,
        SmtpSettingsClient smtpSettingsClient,
        @Value("${notification.mail.enabled:false}") boolean fallbackMailEnabled,
        @Value("${notification.mail.from:no-reply@tsatech.local}") String fallbackFromAddress,
        @Value("${MAIL_HOST:}") String fallbackHost,
        @Value("${MAIL_PORT:25}") Integer fallbackPort,
        @Value("${MAIL_USERNAME:}") String fallbackUsername,
        @Value("${MAIL_PASSWORD:}") String fallbackPassword,
        @Value("${MAIL_SMTP_AUTH:false}") boolean fallbackSmtpAuth,
        @Value("${MAIL_SMTP_STARTTLS:false}") boolean fallbackSmtpStarttls
    ) {
        this.messageSource = messageSource;
        this.smtpSettingsClient = smtpSettingsClient;
        this.fallbackMailEnabled = fallbackMailEnabled;
        this.fallbackFromAddress = fallbackFromAddress;
        this.fallbackHost = fallbackHost;
        this.fallbackPort = fallbackPort;
        this.fallbackUsername = fallbackUsername;
        this.fallbackPassword = fallbackPassword;
        this.fallbackSmtpAuth = fallbackSmtpAuth;
        this.fallbackSmtpStarttls = fallbackSmtpStarttls;
    }

    public boolean sendOrderConfirmation(OrderConfirmationRequest request) {
        if (request == null || isBlank(request.getCustomerEmail()) || request.getOrderId() == null) {
            return false;
        }

        SmtpRuntimeSettings smtp = resolveSmtpSettings();
        if (!Boolean.TRUE.equals(smtp.getSmtpEnabled())) {
            logger.info("SMTP disabled. Order confirmation prepared for order {}", request.getOrderId());
            return true;
        }

        Locale locale = resolveLocale(request.getLocale());
        String subject = messageSource.getMessage(
            "mail.order.subject",
            new Object[] {request.getOrderId()},
            "Order confirmation",
            locale
        );

        String body = buildBody(request, locale);

        try {
            JavaMailSenderImpl sender = buildSender(smtp);
            SimpleMailMessage message = new SimpleMailMessage();
            String fromAddress = firstNonBlank(smtp.getMailFromEmail(), smtp.getSmtpUsername(), fallbackFromAddress);
            message.setFrom(fromAddress);
            message.setTo(request.getCustomerEmail());
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            logger.info("Order confirmation mail sent to {} for order {}", request.getCustomerEmail(), request.getOrderId());
            return true;
        } catch (Exception ex) {
            logger.warn("Unable to send order confirmation mail for order {}: {}", request.getOrderId(), ex.getMessage());
            return false;
        }
    }

    private SmtpRuntimeSettings resolveSmtpSettings() {
        SmtpRuntimeSettings runtime = smtpSettingsClient.fetchRuntimeSettings();
        if (runtime != null && !isBlank(runtime.getSmtpHost())) {
            if (runtime.getSmtpPort() == null || runtime.getSmtpPort() <= 0) {
                runtime.setSmtpPort(587);
            }
            if (runtime.getSmtpAuth() == null) {
                runtime.setSmtpAuth(Boolean.TRUE);
            }
            if (runtime.getSmtpStarttls() == null) {
                runtime.setSmtpStarttls(Boolean.TRUE);
            }
            if (runtime.getSmtpEnabled() == null) {
                runtime.setSmtpEnabled(Boolean.TRUE);
            }
            return runtime;
        }

        SmtpRuntimeSettings fallback = new SmtpRuntimeSettings();
        fallback.setSmtpEnabled(fallbackMailEnabled);
        fallback.setSmtpHost(fallbackHost);
        fallback.setSmtpPort(fallbackPort != null ? fallbackPort : 25);
        fallback.setSmtpUsername(fallbackUsername);
        fallback.setSmtpPassword(fallbackPassword);
        fallback.setSmtpAuth(fallbackSmtpAuth);
        fallback.setSmtpStarttls(fallbackSmtpStarttls);
        fallback.setMailFromEmail(fallbackFromAddress);
        fallback.setMailFromName("TSATech Store");
        return fallback;
    }

    private JavaMailSenderImpl buildSender(SmtpRuntimeSettings smtp) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getSmtpHost());
        sender.setPort(smtp.getSmtpPort() != null ? smtp.getSmtpPort() : 25);
        sender.setUsername(firstNonBlank(smtp.getSmtpUsername(), fallbackUsername));
        sender.setPassword(firstNonBlank(smtp.getSmtpPassword(), fallbackPassword));

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(smtp.getSmtpAuth())));
        properties.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(smtp.getSmtpStarttls())));
        return sender;
    }

    private String buildBody(OrderConfirmationRequest request, Locale locale) {
        StringBuilder sb = new StringBuilder();
        String name = !isBlank(request.getCustomerName()) ? request.getCustomerName() : request.getCustomerEmail();

        sb.append(messageSource.getMessage("mail.order.greeting", new Object[] {name}, "Hello", locale)).append("\n\n");
        sb.append(messageSource.getMessage("mail.order.thanks", null, "Thanks for your order.", locale)).append("\n");
        sb.append(messageSource.getMessage("mail.order.number", new Object[] {request.getOrderId()}, "Order", locale)).append("\n\n");

        List<OrderConfirmationItem> items = request.getItems();
        if (items != null && !items.isEmpty()) {
            sb.append(messageSource.getMessage("mail.order.items", null, "Items", locale)).append(":\n");
            for (OrderConfirmationItem item : items) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                BigDecimal lineTotal = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
                sb.append("- ").append(item.getName() != null ? item.getName() : "Item")
                    .append(" x").append(qty)
                    .append(" = ").append(formatMoney(lineTotal, request.getCurrency(), locale))
                    .append("\n");
            }
            sb.append("\n");
        }

        sb.append(messageSource.getMessage(
            "mail.order.total",
            new Object[] {formatMoney(request.getTotal(), request.getCurrency(), locale)},
            "Total",
            locale
        )).append("\n");

        if (!isBlank(request.getStoreUrl())) {
            sb.append(messageSource.getMessage("mail.order.store", null, "Store", locale)).append(": ")
                .append(request.getStoreUrl()).append("\n");
        }

        sb.append("\n").append(messageSource.getMessage("mail.order.signature", null, "TSATech Store", locale));
        return sb.toString();
    }

    private String formatMoney(BigDecimal value, String currency, Locale locale) {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        if (currency != null && currency.length() == 3) {
            format.setCurrency(java.util.Currency.getInstance(currency.toUpperCase(Locale.ROOT)));
        }
        return format.format(value != null ? value : BigDecimal.ZERO);
    }

    private Locale resolveLocale(String lang) {
        if (lang == null || lang.isBlank()) {
            return Locale.ENGLISH;
        }
        String normalized = lang.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "it" -> Locale.ITALIAN;
            case "fr" -> Locale.FRENCH;
            case "de" -> Locale.GERMAN;
            case "es" -> new Locale("es");
            default -> Locale.ENGLISH;
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
