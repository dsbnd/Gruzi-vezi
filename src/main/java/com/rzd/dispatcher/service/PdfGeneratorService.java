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
    private static final String FONT_PATH = "src/main/resources/a3arialrusnormal.ttf";

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
        document.add(new Paragraph("Руководитель ____________________ (Харитонова А.А.)", fontNormal));
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
        // Увеличиваем отступы для солидности
        Document document = new Document(PageSize.A4, 60, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, out);

        document.open();

        byte[] fontData = new ClassPathResource("a3arialrusnormal.ttf").getInputStream().readAllBytes();
        BaseFont bf = BaseFont.createFont("a3arialrusnormal.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontData, null);

        Font fontTitle = new Font(bf, 14, Font.BOLD);
        Font fontSection = new Font(bf, 11, Font.BOLD);
        Font fontNormal = new Font(bf, 10, Font.NORMAL);
        Font fontSmall = new Font(bf, 8, Font.NORMAL);

        // 1. Заголовок
        Paragraph title = new Paragraph("Договор № " + order.getId().toString().substring(0, 8).toUpperCase() +
                "\nоб организации перевозок грузов железнодорожным транспортом", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 2. Место и дата
        PdfPTable datePlace = new PdfPTable(2);
        datePlace.setWidthPercentage(100);
        addBorderlessCell(datePlace, "г. Москва", fontNormal, Element.ALIGN_LEFT);
        addBorderlessCell(datePlace, LocalDate.now().toString() + " г.", fontNormal, Element.ALIGN_RIGHT);
        document.add(datePlace);
        document.add(new Paragraph("\n"));

        // 3. Преамбула
        addText(document, String.format(
                "Открытое акционерное общество «Российские железные дороги» (ОАО «РЖД»), именуемое в дальнейшем «Перевозчик», " +
                        "в лице начальника службы движения Харитоновой А.А., действующей на основании Доверенности №РЖД-124/д, с одной стороны, и " +
                        "Общество с ограниченной ответственностью «%s» (ИНН %s), именуемое в дальнейшем «Грузоотправитель», " +
                        "в лице Генерального директора, действующего на основании Устава, совместно именуемые «Стороны», " +
                        "заключили настоящий Договор о нижеследующем:",
                order.getUser().getCompanyName(), order.getUser().getInn()
        ), fontNormal);

        // 4. Разделы
        addSection(document, "1. Предмет договора", fontSection);
        addText(document, String.format(
                "1.1. Перевозчик обязуется осуществить перевозку вверенного ему Грузоотправителем груза (%s, вес %d кг.) " +
                        "из пункта отправления ст. %s в пункт назначения ст. %s.",
                order.getCargo().getCargoType(), order.getCargo().getWeightKg(),
                order.getDepartureStation(), order.getDestinationStation()
        ), fontNormal);
        addText(document, "1.2. Грузоотправитель обязуется оплатить услуги по перевозке и предоставлению подвижного состава в соответствии с тарифами, утвержденными Прейскурантом № 10-01.", fontNormal);

        addSection(document, "2. Права и обязанности сторон", fontSection);
        addText(document, "2.1. Перевозчик обязан подать под погрузку исправный, очищенный от остатков предыдущего груза подвижной состав (вагон типа: " + order.getRequestedWagonType() + ").", fontNormal);
        addText(document, "2.2. Грузоотправитель обязан обеспечить подготовку груза к перевозке таким образом, чтобы обеспечить безопасность движения и сохранность груза в пути следования.", fontNormal);
        addText(document, "2.3. Стороны обязаны соблюдать требования Правил перевозок грузов железнодорожным транспортом и Устава железнодорожного транспорта Российской Федерации.", fontNormal);

        addSection(document, "3. Порядок расчетов", fontSection);
        addText(document, "3.1. Ориентировочная стоимость услуг по настоящему Договору составляет: " + order.getTotalPrice() + " руб., в том числе НДС 20%.", fontNormal);
        addText(document, "3.2. Оплата производится Грузоотправителем в порядке 100% предоплаты не позднее 3-х банковских дней с момента выставления счета.", fontNormal);

        addSection(document, "4. Ответственность сторон", fontSection);
        addText(document, "4.1. За неисполнение или ненадлежащее исполнение обязательств по настоящему Договору Стороны несут ответственность в соответствии с законодательством РФ.", fontNormal);
        addText(document, "4.2. Перевозчик несет ответственность за сохранность груза после принятия его к перевозке и до выдачи его получателю, если не докажет, что утрата или повреждение груза произошли вследствие обстоятельств, которые Перевозчик не мог предотвратить.", fontNormal);

        // Вставляем разрыв страницы
        document.newPage();

        addSection(document, "5. Форс-мажор", fontSection);
        addText(document, "5.1. Стороны освобождаются от ответственности за частичное или полное неисполнение обязательств по Договору, если оно явилось следствием обстоятельств непреодолимой силы (пожар, наводнение, землетрясение, военные действия, эпидемии).", fontNormal);

        addSection(document, "6. Срок действия договора", fontSection);
        addText(document, "6.1. Настоящий Договор вступает в силу с момента его подписания и действует до полного исполнения Сторонами своих обязательств.", fontNormal);
        addText(document, "6.2. Все споры и разногласия, возникающие в процессе исполнения Договора, решаются путем переговоров, а при недостижении согласия — в Арбитражном суде г. Москвы.", fontNormal);

        // 7. Реквизиты
        document.add(new Paragraph("\n\n7. Юридические адреса и реквизиты сторон\n\n", fontSection));
        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);

        // Левая колонка - РЖД
        PdfPCell rzdCell = new PdfPCell();
        rzdCell.setBorder(Rectangle.NO_BORDER);
        rzdCell.addElement(new Paragraph("Перевозчик:", fontSection));
        rzdCell.addElement(new Paragraph("ОАО «РЖД»", fontNormal));
        rzdCell.addElement(new Paragraph("ИНН: 7708503727 / КПП: 770801001", fontSmall));
        rzdCell.addElement(new Paragraph("БИК: 044525187", fontSmall));
        rzdCell.addElement(new Paragraph("\n\n________________ / Харитонова А.А. /", fontNormal));
        footer.addCell(rzdCell);

        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.NO_BORDER);
        clientCell.addElement(new Paragraph("Грузоотправитель:", fontSection));
        clientCell.addElement(new Paragraph(order.getUser().getCompanyName(), fontNormal));
        clientCell.addElement(new Paragraph("ИНН: " + order.getUser().getInn(), fontSmall));
        rzdCell.addElement(new Paragraph(" ", fontSmall));
        clientCell.addElement(new Paragraph("\n\n\n________________ / ____________ /", fontNormal));
        footer.addCell(clientCell);

        document.add(footer);
        byte[] signABytes = new ClassPathResource("signA.png").getInputStream().readAllBytes();
        Image sigA = Image.getInstance(signABytes);
        sigA.scaleToFit(100, 50);
        sigA.setAbsolutePosition(70f, 440f); // Координаты X и Y
        document.add(sigA);
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