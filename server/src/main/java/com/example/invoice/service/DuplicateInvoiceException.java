package com.example.invoice.service;

public class DuplicateInvoiceException extends RuntimeException {
    private final String invoiceNumber;

    public DuplicateInvoiceException(String invoiceNumber) {
        super("发票号码已存在: " + invoiceNumber);
        this.invoiceNumber = invoiceNumber;
    }

    public String getInvoiceNumber() { return invoiceNumber; }
}
