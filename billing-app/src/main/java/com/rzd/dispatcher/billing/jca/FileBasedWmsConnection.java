package com.rzd.dispatcher.billing.jca;

import jakarta.resource.ResourceException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class FileBasedWmsConnection implements WmsConnection {

    @Override
    public void sendShippingNote(String orderId, String xmlContent) throws ResourceException {
        log.info("[JCA/WMS] Отправка накладной для заказа {} в Систему Складского Учёта...", orderId);

        try {
            Path outboxDir = Paths.get("wms_outbox");
            if (!Files.exists(outboxDir)) {
                Files.createDirectories(outboxDir);
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String filename = String.format("shipping_note_%s_%s.xml", orderId, timestamp);
            Path filePath = outboxDir.resolve(filename);
            Files.writeString(filePath, xmlContent);
            log.info("[JCA/WMS] Накладная сохранена в {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[JCA/WMS] Ошибка при сохранении накладной", e);
            throw new ResourceException("Не удалось отправить накладную в WMS", e);
        }
    }

    @Override
    public void close() throws ResourceException {
        log.debug("JCA‑соединение с WMS закрыто");
    }
}