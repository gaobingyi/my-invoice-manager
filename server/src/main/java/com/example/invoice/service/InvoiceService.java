package com.example.invoice.service;

import com.example.invoice.entity.Invoice;
import com.example.invoice.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final SecureRandom RNG = new SecureRandom();

    private final InvoiceRepository repository;
    private final com.example.invoice.service.InvoiceParser parser;
    private final Path uploadDir;

    public InvoiceService(InvoiceRepository repository, com.example.invoice.service.InvoiceParser parser,
                          @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.repository = repository;
        this.parser = parser;
        this.uploadDir = Paths.get(uploadDir);
    }

    public Invoice upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".pdf")) {
            throw new IllegalArgumentException("仅支持 PDF");
        }
        Files.createDirectories(uploadDir);

        // pocfile: store to a temp name first because seller/invoice-number are only known
        // after parsing; rename to <seller>_<number>.<ext> afterwards.
        Path tmp = uploadDir.resolve(UUID.randomUUID() + ".pdf");
        Files.copy(file.getInputStream(), tmp, StandardCopyOption.REPLACE_EXISTING);
        ParsedInvoice p;
        try {
            p = parser.parse(tmp);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);          // pocfile: a parse failure must not leak the temp file
            throw e;
        }

        // pocfile: duplicate invoice number should be rejected. If parsing produced no
        // number we keep the file but store a sentinel so the NOT NULL/UNIQUE columns hold.
        // The sentinel must fit the VARCHAR(20) column, hence the truncated UUID suffix.
        String number = p.invoiceNumber();
        // pocfile: 12 hex chars = 48 bits CSPRNG, plenty for sentinel uniqueness.
        // UUID.randomUUID().toString() 全 32 字符再 substring 是浪费：每次分配 + 格式化。
        String sentinel = number != null ? null
                : "UNKNOWN-" + String.format("%012x", RNG.nextLong() & 0xFFFFFFFFFFFFL);

        String filename = buildFilename(p, name, sentinel);
        Path dest = uploadDir.resolve(filename);
        if (!dest.equals(tmp)) {
            try {
                // pocfile: no REPLACE_EXISTING — a duplicate upload must 409, not clobber
                // the previously stored PDF the surviving row still points at.
                Files.move(tmp, dest);
            } catch (FileAlreadyExistsException e) {
                Files.deleteIfExists(tmp);
                throw new DuplicateInvoiceException(number);
            }
        }

        Invoice inv = new Invoice();
        inv.setInvoiceNumber(number != null ? number : sentinel);
        inv.setInvoiceDate(p.invoiceDate());
        inv.setBuyerName(p.buyerName());
        inv.setBuyerTaxId(p.buyerTaxId());
        inv.setSellerName(p.sellerName());
        inv.setSellerTaxId(p.sellerTaxId());
        inv.setCategory(p.category());
        inv.setTotalAmount(p.totalAmount());
        inv.setTaxAmount(p.taxAmount());
        inv.setTotalWithTax(p.totalWithTax());
        inv.setDrawer(p.drawer());
        inv.setFilePath(uploadDir.relativize(dest).toString());

        try {
            return repository.save(inv);
        } catch (DataIntegrityViolationException e) {
            Files.deleteIfExists(dest);
            // pocfile: the file we just moved is a fresh copy (line 69) and the surviving row
            // points at it; delete it and report the duplicate. The caller may still decide to
            // overwrite with a new copy — but never let us leave the row pointing at nothing.
            if (p.invoiceNumber() != null) {
                throw new DuplicateInvoiceException(p.invoiceNumber());
            }
            // pocfile: two concurrent unparseable uploads collide on the same dest file;
            // both are UNKNOWN-numbered so keep the first file and 409 the second.
            throw new DuplicateInvoiceException("UNKNOWN");
        }
    }

    public Page<Invoice> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public Path resolveFile(Long id) {
        Invoice inv = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("发票不存在: " + id));
        return uploadDir.resolve(inv.getFilePath()).normalize();
    }

    private String buildFilename(ParsedInvoice p, String originalName, String sentinel) {
        String seller = p.sellerName() == null ? "UNKNOWN" : p.sellerName();
        String number = p.invoiceNumber() != null ? p.invoiceNumber() : sentinel;
        // pocfile: strip path separators and OS-reserved characters so the filename can
        // never escape uploadDir or break the filesystem.
        String safe = (seller + "_" + number).replaceAll("[\\\\/:*?\"<>|\\r\\n\\t ]+", "_");
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".pdf";
        return safe + ext;
    }

    public void delete(Long id) throws IOException {
        Invoice inv = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("发票不存在: " + id));
        // pocfile: delete the file first — if removing the DB row then fails the user can
        // retry; deleting the row first orphanes the PDF forever when the file delete fails.
        Files.deleteIfExists(uploadDir.resolve(inv.getFilePath()).normalize());
        repository.delete(inv);
    }
}
