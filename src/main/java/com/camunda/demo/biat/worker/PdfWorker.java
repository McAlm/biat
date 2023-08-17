package com.camunda.demo.biat.worker;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.camunda.demo.biat.storage.StorageProperties;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;

@Component
public class PdfWorker {

    private final Path rootLocation;

    @Autowired
    public PdfWorker(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @JobWorker(type = "pdf")
    public void createPdf(@VariablesAsType EmployeeOnboardingData data) throws Exception {

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(
                this.rootLocation.resolve(data.getEmployeeName() + "_" + data.getDepartment() + ".pdf").toString()));

        document.open();
        Paragraph paragraph = new Paragraph();
        Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
        Chunk chunk = new Chunk("Employee Name: ", font);
        paragraph.add(chunk);
        chunk = new Chunk(data.getEmployeeName(), font);
        paragraph.add(chunk);

        paragraph.add("\n");

        chunk = new Chunk("Department: ", font);
        paragraph.add(chunk);
        chunk = new Chunk(data.getDepartment(), font);
        paragraph.add(chunk);

        paragraph.add("\n");

        document.add(paragraph);

        Path path = Paths.get(rootLocation.resolve(data.getFile()).toUri());
        Image img = Image.getInstance(path.toAbsolutePath().toString());
        document.add(img);
        document.close();
    }
}
