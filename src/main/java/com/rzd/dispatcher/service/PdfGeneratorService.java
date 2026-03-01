package com.rzd.dispatcher.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.model.entity.Payment;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

@Service
public class PdfGeneratorService {
    private static final String FONT_PATH = "src/main/resources/arial3.ttf";

    public byte[] generateInvoicePdf(Payment payment) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        BaseFont bf = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font fontTitle = new Font(bf, 14, Font.BOLD);
        Font fontBold = new Font(bf, 9, Font.BOLD);
        Font fontNormal = new Font(bf, 9, Font.NORMAL);
        Font fontSmall = new Font(bf, 7, Font.NORMAL);

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 4});

        try {
            Image logo = Image.getInstance(new ClassPathResource("logo.png").getURL());
            logo.scaleToFit(80, 80);
            PdfPCell logoCell = new PdfPCell(logo);
            logoCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(logoCell);
        } catch (Exception e) {
            headerTable.addCell(new Phrase("РЖД", fontBold));
        }

        PdfPCell titleCell = new PdfPCell(new Phrase("Внимание! Оплата данного счета означает согласие с условиями поставки товара/оказания услуг.", fontSmall));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        headerTable.addCell(titleCell);
        document.add(headerTable);

        // 2. Банковская сетка (ГОСТ)
        PdfPTable bankGrid = new PdfPTable(4);
        bankGrid.setWidthPercentage(100);
        bankGrid.setWidths(new float[]{2, 1, 0.5f, 1.5f});

        addCell(bankGrid, payment.getBankName(), fontNormal, 2, 1);
        addCell(bankGrid, "БИК", fontNormal, 1, 1);
        addCell(bankGrid, payment.getBik(), fontNormal, 1, 1);

        addCell(bankGrid, "Банк получателя", fontSmall, 2, 1);
        addCell(bankGrid, "Сч. №", fontNormal, 1, 1);
        addCell(bankGrid, "40702810123456789012", fontNormal, 1, 1); // Твой счет РЖД

        addCell(bankGrid, "ИНН 7708503727", fontNormal, 1, 1);
        addCell(bankGrid, "КПП 770801001", fontNormal, 1, 1);
        addCell(bankGrid, "Сч. №", fontNormal, 1, 1);
        addCell(bankGrid, "30101810400000000225", fontNormal, 1, 1); // Корр. счет

        addCell(bankGrid, "ОАО РЖД", fontNormal, 2, 1);
        addCell(bankGrid, "", fontNormal, 1, 2); // Пустая ячейка для красоты
        addCell(bankGrid, "", fontNormal, 1, 2);

        addCell(bankGrid, "Получатель", fontSmall, 2, 1);
        document.add(bankGrid);

        // 3. Номер счета
        Paragraph p = new Paragraph("\nСчет на оплату № " + payment.getPaymentDocument() + " от " + payment.getCreatedAt().toLocalDate(), fontTitle);
        p.setSpacingAfter(10f);
        document.add(p);
        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator()));

        // 4. Поставщик / Покупатель
        document.add(new Paragraph("Поставщик: ОАО РЖД, ИНН 7708503727, КПП 770801001, 107174, Москва г, Новая Басманная ул, дом № 2", fontNormal));
        document.add(new Paragraph("Покупатель: " + payment.getCompanyName() + ", ИНН " + payment.getInn() + ", " + (payment.getKpp() != null ? "КПП " + payment.getKpp() : ""), fontNormal));
        document.add(new Paragraph("\n"));

        // 5. Таблица услуг
        PdfPTable mainTable = new PdfPTable(5);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{0.5f, 5, 1, 1.5f, 1.5f});

        addHeaderCell(mainTable, "№", fontBold);
        addHeaderCell(mainTable, "Товары (работы, услуги)", fontBold);
        addHeaderCell(mainTable, "Кол-во", fontBold);
        addHeaderCell(mainTable, "Цена", fontBold);
        addHeaderCell(mainTable, "Сумма", fontBold);

        addCell(mainTable, "1", fontNormal);
        addCell(mainTable, payment.getPaymentPurpose(), fontNormal);
        addCell(mainTable, "1", fontNormal);
        addCell(mainTable, payment.getAmount().toString(), fontNormal);
        addCell(mainTable, payment.getAmount().toString(), fontNormal);

        document.add(mainTable);

        document.add(new Paragraph("\nИтого: " + payment.getAmount() + " руб.", fontBold));
        document.add(new Paragraph("В том числе НДС (20%): " + payment.getAmount().multiply(new java.math.BigDecimal("0.2")).setScale(2, java.math.RoundingMode.HALF_UP) + " руб.", fontNormal));


        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph("Руководитель ____________________ (Харитонова А.Е.)", fontNormal));
        document.add(new Paragraph("\nГлавный бухгалтер ____________________ (Бондаренко Д.А.)", fontNormal));

        try {
            byte[] signABytes = new ClassPathResource("signA.png").getInputStream().readAllBytes();
            Image sigA = Image.getInstance(signABytes);
            sigA.scaleToFit(100, 50);
            sigA.setAbsolutePosition(90f, 470f); // Координаты X и Y
            document.add(sigA);

            // Если вторая подпись называется signB.png
            byte[] signBBytes = new ClassPathResource("signB.png").getInputStream().readAllBytes();
            Image sigB = Image.getInstance(signBBytes);
            sigB.scaleToFit(100, 50);
            sigB.setAbsolutePosition(135f, 435f);
            document.add(sigB);

            System.out.println("Подписи успешно добавлены в PDF");
        } catch (IOException e) {
            System.out.println("Не удалось найти файл подписи в resources: {}");
        } catch (DocumentException e) {
            System.out.println("Ошибка при добавлении изображения в документ: {}");
        }

        document.close();
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new PdfPCell(new Phrase(text, font)));
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    public byte[] generateContractPdf(Order order) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 50, 40, 50, 50); // Левое поле больше для подшивки
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Загрузка шрифта (используем тот же метод через ClassPathResource, что и в счете)
        byte[] fontData = new ClassPathResource("a3arialrusnormal.ttf").getInputStream().readAllBytes();
        BaseFont bf = BaseFont.createFont("a3arialrusnormal.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontData, null);

        Font fontTitle = new Font(bf, 14, Font.BOLD);
        Font fontSection = new Font(bf, 11, Font.BOLD);
        Font fontNormal = new Font(bf, 10, Font.NORMAL);

        // 1. Заголовок
        Paragraph title = new Paragraph("ДОГОВОР ОРГАНИЗАЦИИ ПЕРЕВОЗКИ ГРУЗА № " +
                order.getId().toString().substring(0, 8).toUpperCase(), fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 2. Место и дата
        PdfPTable datePlace = new PdfPTable(2);
        datePlace.setWidthPercentage(100);
        addBorderlessCell(datePlace, "г. Москва", fontNormal, Element.ALIGN_LEFT);
        addBorderlessCell(datePlace, LocalDate.now().toString(), fontNormal, Element.ALIGN_RIGHT);
        document.add(datePlace);
        document.add(new Paragraph("\n"));

        // 3. Преамбула
        String preamble = String.format(
                "ОАО «РЖД», именуемое в дальнейшем «Перевозчик», в лице начальника службы движения, с одной стороны, и " +
                        "компания %s (ИНН %s), именуемая в дальнейшем «Грузоотправитель», в лице руководителя, действующего на основании Устава, " +
                        "совместно именуемые «Стороны», заключили настоящий Договор о нижеследующем:",
                order.getUser().getCompanyName(), order.getUser().getInn()
        );
        Paragraph pInfo = new Paragraph(preamble, fontNormal);
        pInfo.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(pInfo);

        // 4. Разделы договора
        addSection(document, "1. ПРЕДМЕТ ДОГОВОРА", fontSection);
        addText(document, String.format(
                "1.1. Перевозчик обязуется осуществить перевозку груза (%s) весом %d кг по маршруту: ст. %s — ст. %s.",
                order.getCargo().getCargoType(), order.getCargo().getWeightKg(),
                order.getDepartureStation(), order.getDestinationStation()
        ), fontNormal);
        addText(document, "1.2. Грузоотправитель обязуется оплатить услуги Перевозчика согласно установленным тарифам.", fontNormal);

        addSection(document, "2. ОБЯЗАННОСТИ СТОРОН", fontSection);
        addText(document, "2.1. Перевозчик обязан подать исправный подвижной состав (тип: " + order.getRequestedWagonType() + ") под погрузку в установленный срок.", fontNormal);
        addText(document, "2.2. Грузоотправитель обязан обеспечить правильность заполнения перевозочных документов и соответствие упаковки груза (" + order.getCargo().getPackagingType() + ") требованиям безопасности.", fontNormal);

        addSection(document, "3. СТОИМОСТЬ И ПОРЯДОК РАСЧЕТОВ", fontSection);
        addText(document, "3.1. Стоимость перевозки определяется согласно расчету и составляет: " + order.getTotalPrice() + " руб.", fontNormal);
        addText(document, "3.2. Оплата производится на условиях 100% предоплаты.", fontNormal);

        // 5. Реквизиты и подписи (Две колонки)
        document.add(new Paragraph("\n\n4. РЕКВИЗИТЫ СТОРОН\n\n", fontSection));
        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);

        // Колонка РЖД
        String rzdInfo = "ПЕРЕВОЗЧИК:\nОАО «РЖД»\nИНН 7708503727\nг. Москва, ул. Новая Басманная, 2\n\n\n________________ /Белозёров О.В./";
        addBorderlessCell(footer, rzdInfo, fontNormal, Element.ALIGN_LEFT);

        // Колонка Клиента
        String clientInfo = String.format("ГРУЗООТПРАВИТЕЛЬ:\n%s\nИНН %s\n\n\n\n________________ / ____________ /",
                order.getUser().getCompanyName(), order.getUser().getInn());
        addBorderlessCell(footer, clientInfo, fontNormal, Element.ALIGN_LEFT);

        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    // Вспомогательные методы для чистоты кода
    private void addSection(Document doc, String title, Font font) throws DocumentException {
        Paragraph p = new Paragraph("\n" + title, font);
        p.setSpacingAfter(5);
        doc.add(p);
    }

    private void addText(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        doc.add(p);
    }

    private void addBorderlessCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }
}