package com.rzd.dispatcher.billing.jca;

import lombok.extern.slf4j.Slf4j;

import jakarta.resource.ResourceException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Slf4j
public class GoogleSheetsEisConnection implements WmsConnection {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Gruzi-Vezi-Billing";
    private final String spreadsheetId;
    private final String sheetName;
    private Sheets sheetsService;

    public GoogleSheetsEisConnection(String spreadsheetId, String sheetName, String credentialsFilePath) throws ResourceException {
        this.spreadsheetId = spreadsheetId;
        this.sheetName = sheetName;
        try {
            this.sheetsService = getSheetsService(credentialsFilePath);
        } catch (Exception e) {
            log.error("Не удалось инициализировать сервис Google Sheets", e);
            throw new ResourceException("Google Sheets connection failed", e);
        }
    }

    private static Sheets getSheetsService(String credentialsFilePath) throws Exception {
        try (InputStream in = GoogleSheetsEisConnection.class.getResourceAsStream(credentialsFilePath)) {
            if (in == null) {
                throw new IOException("Файл с ключами не найден: " + credentialsFilePath);
            }
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
    }

    @Override
    public void sendShippingNote(String orderId, String jsonDetails) throws ResourceException {
        log.info("[JCA/GoogleSheets] Добавление строки о доставке в ЕИС Google Таблицу для заказа {}...", orderId);

        try {
            String totalPrice = "N/A";
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonDetails);
                com.fasterxml.jackson.databind.JsonNode nodeTotalPrice = root.get("totalPrice");
                if (nodeTotalPrice != null && !nodeTotalPrice.isNull()) {
                    totalPrice = new BigDecimal(nodeTotalPrice.toString()).toPlainString() + " руб.";
                }
            } catch (Exception e) {
                log.warn("Не удалось извлечь сумму из деталей заказа {}", orderId);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String status = "ДОСТАВЛЕН";

            ValueRange body = new ValueRange()
                    .setValues(Collections.singletonList(
                            Arrays.asList(timestamp, orderId, totalPrice, status)
                    ));

            String range = sheetName + "!A:D";
            this.sheetsService.spreadsheets().values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

            log.info("[JCA/GoogleSheets] Строка для заказа {} успешно добавлена в ЕИС Google Sheets!", orderId);

        } catch (Exception e) {
            log.error("[JCA/GoogleSheets] Ошибка при добавлении строки в таблицу", e);
            throw new ResourceException("Не удалось отправить данные в Google Sheets", e);
        }
    }

    @Override
    public void close() throws ResourceException {
        log.debug("Соединение JCA-GoogleSheets закрыто");
        this.sheetsService = null;
    }
}