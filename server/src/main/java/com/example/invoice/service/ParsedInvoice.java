package com.example.invoice.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedInvoice(
        String invoiceNumber,
        LocalDate invoiceDate,
        String buyerName,
        String buyerTaxId,
        String sellerName,
        String sellerTaxId,
        String category,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal totalWithTax,
        String drawer
) {}
