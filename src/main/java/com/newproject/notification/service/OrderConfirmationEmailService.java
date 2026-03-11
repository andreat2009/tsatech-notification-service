package com.newproject.notification.service;

import com.newproject.notification.dto.OrderConfirmationItem;
import com.newproject.notification.dto.OrderConfirmationRequest;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OrderConfirmationEmailService {
    private static final Logger logger = LoggerFactory.getLogger(OrderConfirmationEmailService.class);

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;
    private final boolean mailEnabled;
    private final String fromAddress;

    public OrderConfirmationEmailService(
        JavaMailSender mailSender,
        MessageSource messageSource,
        @Value("${notification.mail.enabled:false}") boolean mailEnabled,
        @Value("${notification.mail.from:no-reply@tsatech.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    public boolean sendOrderConfirmation(OrderConfirmationRequest request) {
        if (request == null || isBlank(request.getCustomerEmail()) || request.getOrderId() == null) {
            return false;
        }

        Locale locale = resolveLocale(request.getLocale());
        String subject = messageSource.getMessage(
            "mail.order.subject",
            new Object[] {request.getOrderId()},
            "Order confirmation",
            locale
        );

        String body = buildBody(request, locale);

        if (!mailEnabled) {
            logger.info("Mail disabled. Order confirmation prepared for {}: {}", request.getCustomerEmail(), subject);
            logger.info("Mail body:\n{}", body);
            return true;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.getCustomerEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Order confirmation mail sent to {} for order {}", request.getCustomerEmail(), request.getOrderId());
            return true;
        } catch (Exception ex) {
            logger.warn("Unable to send order confirmation mail for order {}: {}", request.getOrderId(), ex.getMessage());
            return false;
        }
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
