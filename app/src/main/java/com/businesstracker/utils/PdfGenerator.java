package com.businesstracker.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.businesstracker.R;
import com.businesstracker.models.Business;
import com.businesstracker.models.Expense;
import com.businesstracker.models.Sale;
import com.businesstracker.models.Task;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfGenerator {

    public static File generateBusinessReport(Context context, Business business, List<Expense> expenses, List<Task> tasks) throws Exception {
        File file = new File(context.getExternalFilesDir(null), business.getName().replaceAll("\\s+", "_") + "_Report.pdf");
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        addHeader(context, document);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD);
        Font subTitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.NORMAL, BaseColor.GRAY);

        Paragraph title = new Paragraph(business.getName(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph desc = new Paragraph(business.getDescription(), subTitleFont);
        desc.setAlignment(Element.ALIGN_CENTER);
        desc.setSpacingAfter(20f);
        document.add(desc);

        addBusinessSection(document, business, expenses, tasks);

        document.close();
        return file;
    }

    public static File generateFullReport(Context context, List<Business> businesses, List<Expense> allExpenses, List<Task> allTasks, List<Sale> allSales) throws Exception {
        File file = new File(context.getExternalFilesDir(null), "Inventory_Valuation_Report.pdf");
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // 1. MAIN HEADER
        Font companyFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font reportTitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font dateFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        Paragraph p1 = new Paragraph("QUICKSILVER DRUG", companyFont);
        p1.setAlignment(Element.ALIGN_RIGHT);
        document.add(p1);

        Paragraph p2 = new Paragraph("MERCHANDISE", reportTitleFont);
        p2.setAlignment(Element.ALIGN_RIGHT);
        document.add(p2);

        String today = new SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(new Date());
        Paragraph p3 = new Paragraph("As of " + today, dateFont);
        p3.setAlignment(Element.ALIGN_RIGHT);
        p3.setSpacingAfter(10f);
        document.add(p3);

        // 2. MAIN TABLE (The Quicksilver Format)
        PdfPTable table = new PdfPTable(10); // 10 columns to match the complex headers
        table.setWidthPercentage(100);
        float[] columnWidths = {1.2f, 2.5f, 1.0f, 0.6f, 1.0f, 1.2f, 0.8f, 0.8f, 0.8f, 0.8f};
        table.setWidths(columnWidths);

        Font smallHeader = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD);
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

        // Row 1: Main Headers
        addHeaderCell(table, "PRODUCT / INVENTORY CODE", smallHeader, 1, 2);
        addHeaderCell(table, "ITEM DESCRIPTION", smallHeader, 1, 2);
        addHeaderCell(table, "LOCATION (Note 1)", smallHeader, 3, 1);
        addHeaderCell(table, "INVENTORY VALUATION METHOD (Note 2)", smallHeader, 1, 2);
        addHeaderCell(table, "UNIT PRICE", smallHeader, 2, 1);
        addHeaderCell(table, "QUANTITY", smallHeader, 2, 1);

        // Row 2: Sub-headers
        addHeaderCell(table, "ADDRESS", smallHeader, 1, 1);
        addHeaderCell(table, "CODE", smallHeader, 1, 1);
        addHeaderCell(table, "REMARKS", smallHeader, 1, 1);
        addHeaderCell(table, "/CASE", smallHeader, 1, 1);
        addHeaderCell(table, "/PC", smallHeader, 1, 1);
        addHeaderCell(table, "/CASE", smallHeader, 1, 1);
        addHeaderCell(table, "/PC", smallHeader, 1, 1);

        // DATA ROWS
        for (Business b : businesses) {
            // Filter sales for this business
            List<Sale> bSales = new ArrayList<>();
            for (Sale s : allSales) if (s.getBusinessId().equals(b.getId())) bSales.add(s);

            if (bSales.isEmpty()) {
                // Placeholder row if no sales
                addDataCell(table, b.getId().substring(b.getId().length()-6), dataFont);
                addDataCell(table, b.getName() + " (No records)", dataFont);
                addDataCell(table, "-", dataFont); addDataCell(table, "-", dataFont); addDataCell(table, "-", dataFont);
                addDataCell(table, "Cost", dataFont);
                addDataCell(table, "-", dataFont); addDataCell(table, "-", dataFont);
                addDataCell(table, "-", dataFont); addDataCell(table, "-", dataFont);
            } else {
                for (Sale s : bSales) {
                    addDataCell(table, b.getId().substring(b.getId().length()-6), dataFont);
                    addDataCell(table, s.getProductName(), dataFont);
                    addDataCell(table, "Main Whse", dataFont); 
                    addDataCell(table, "A1", dataFont); 
                    addDataCell(table, "Good Condition", dataFont);
                    addDataCell(table, "FIFO", dataFont);
                    addDataCell(table, s.getUnitPrice(), dataFont); // Case
                    addDataCell(table, "-", dataFont); // PC
                    addDataCell(table, s.getQuantity(), dataFont); // Case
                    addDataCell(table, "-", dataFont); // PC
                }
            }
        }

        // Add 5 empty rows for manual entries
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) addDataCell(table, " ", dataFont);
        }

        document.add(table);
        document.close();
        return file;
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private static void addDataCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setMinimumHeight(18f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addHeader(Context context, Document document) throws Exception {
        Drawable d = ContextCompat.getDrawable(context, R.drawable.app_logo);
        if (d != null) {
            Bitmap bitmap;
            if (d instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) d).getBitmap();
            } else {
                int width = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 200;
                int height = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 200;
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                d.draw(canvas);
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            Image logo = Image.getInstance(stream.toByteArray());
            logo.scaleToFit(80, 80);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
        }
    }

    private static void addBusinessSection(Document document, Business business, List<Expense> expenses, List<Task> tasks) throws Exception {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);

        // Summary Section
        document.add(new Paragraph("Financial Summary", new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD)));
        document.add(new Paragraph(String.format(Locale.US, "Target Budget: ₱%,.2f", business.getTargetBudget())));
        document.add(new Paragraph(String.format(Locale.US, "Total Spent: ₱%,.2f", business.getCurrentSpent())));
        document.add(new Paragraph(String.format(Locale.US, "Total Revenue: ₱%,.2f", business.getTotalRevenue())));
        document.add(new Paragraph(String.format(Locale.US, "Profit/Loss: ₱%,.2f", business.getProfit())));
        document.add(new Paragraph(String.format(Locale.US, "Budget Usage: %.1f%%", business.getProgressPercent())));
        document.add(new Paragraph(" "));

        // Expenses Table
        if (!expenses.isEmpty()) {
            document.add(new Paragraph("Expense Records", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            
            addTableCell(table, "Date", headerFont, BaseColor.DARK_GRAY);
            addTableCell(table, "Type", headerFont, BaseColor.DARK_GRAY);
            addTableCell(table, "Description", headerFont, BaseColor.DARK_GRAY);
            addTableCell(table, "Amount", headerFont, BaseColor.DARK_GRAY);

            for (Expense e : expenses) {
                table.addCell(e.getDate());
                table.addCell(e.getType());
                table.addCell(e.getDescription());
                table.addCell(String.format(Locale.US, "₱%,.2f", e.getAmount()));
            }
            document.add(table);
            document.add(new Paragraph(" "));
        }

        // Tasks Table
        if (!tasks.isEmpty()) {
            document.add(new Paragraph("Task Checklist", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            addTableCell(table, "Status", headerFont, BaseColor.DARK_GRAY);
            addTableCell(table, "Task Title", headerFont, BaseColor.DARK_GRAY);
            addTableCell(table, "Due Date", headerFont, BaseColor.DARK_GRAY);

            for (Task t : tasks) {
                table.addCell(t.isCompleted() ? "DONE" : "PENDING");
                table.addCell(t.getTitle());
                table.addCell(t.getDueDate());
            }
            document.add(table);
        }
    }

    private static void addTableCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5f);
        table.addCell(cell);
    }
}
