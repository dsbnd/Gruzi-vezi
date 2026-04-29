package com.rzd.dispatcher.billing.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.Reference;

@Component("googleSheetsConnectionFactory")
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetsConnectionFactory implements WmsConnectionFactory {

    @Value("${google.sheets.spreadsheet.id}")
    private String spreadsheetId;

    @Value("${google.sheets.work.sheet.name}")
    private String sheetName;

    private static final String CREDENTIALS_FILE_PATH = "/google-sheets-credentials.json";

    @Override
    public WmsConnection getConnection() throws ResourceException {
        log.info("Создание нового подключения к Google Sheets...");
        return new GoogleSheetsEisConnection(spreadsheetId, sheetName, CREDENTIALS_FILE_PATH);
    }

    @Override
    public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
        return null;
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return null;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return null;
    }

    @Override
    public void setReference(Reference reference) {

    }

    @Override
    public Reference getReference() throws NamingException {
        return null;
    }
}