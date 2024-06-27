package com.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.example.demo.Constants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public class CsvToPdf {
	public static void main(String[] args) {
		String csvFile = "D:\\Projects\\Ronak\\csvfiles\\";
		String pdfFile = "D:\\Projects\\Ronak\\pdffiles\\";

		CsvToPdf csvToPdf = new CsvToPdf();
		// csvToPdf.migaretCsvToPdfFiles(csvFile, pdfFile);

		String htmlFilePath = "D:\\Projects\\Ronak\\htmlfiles\\myhtml.html";
		csvToPdf.processHtmlFile(htmlFilePath, pdfFile);
	}

	private void processHtmlFile(String htmlFilePath, String pdfFile) {
		String pdfFilePath = pdfFile + File.separator + FilenameUtils.removeExtension(new File(htmlFilePath).getName())
				+ ".pdf";

		try {
            // Read the HTML content from the file using BufferedReader
            BufferedReader reader = new BufferedReader(new FileReader(htmlFilePath));
            StringWriter writer = new StringWriter();
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            reader.close();
            String htmlContent = writer.toString().trim(); // Trim leading/trailing whitespace

            // Remove BOM if present
            if (htmlContent.startsWith("\uFEFF")) {
                htmlContent = htmlContent.substring(1);
            }

            // Ensure HTML content starts with the correct tag
            if (!htmlContent.startsWith("<html>")) {
                throw new IllegalArgumentException("Invalid HTML content");
            }

            // Create a FileOutputStream to write the PDF file
            try (FileOutputStream os = new FileOutputStream(pdfFilePath)) {
                // Create the PDF renderer
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(htmlContent, new File(htmlFilePath).toURI().toString());
                builder.toStream(os);
                builder.run();
            }

            System.out.println("PDF created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	public static void processFiles() {
		List<Path> files = null;
		try {
			files = listFiles("D:\\Projects\\Ronak\\encryptedfiles\\");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Path path : files) {
			String remoteFileName = path.toFile().getName();
			if (true) {
				if (remoteFileName.contains(Constants.FILE_PATTERN)) {

					System.out.println("File downloaded : " + remoteFileName);
				}
			} else {

				System.out.println("File downloaded : " + remoteFileName);
			}
		}
	}

	public void migaretCsvToPdfFiles(String csvFilePath, String pdfFilePath) {
		try {
			List<Path> filePaths = listFiles(csvFilePath);
			for (Path filePath : filePaths) {
				File file = filePath.toFile();
				PdfWriter writer = new PdfWriter(
						pdfFilePath + File.separator + FilenameUtils.removeExtension(file.getName()) + ".pdf");

				PdfDocument pdfDoc = new PdfDocument(writer);

				Document document = new Document(pdfDoc);

				Path path = Paths.get(file.getAbsolutePath());
				List<String> lines = Files.readAllLines(path);

				if (!lines.isEmpty()) {
					String[] headers = lines.get(0).split(",");
					Table table = new Table(headers.length);

					for (String header : headers) {
						Cell headerCell = new Cell().add(new Paragraph(header).setBold())
								.setBackgroundColor(new DeviceRgb(221, 221, 221)) // light grey background
								.setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
								.setBorder(new SolidBorder(1));
						table.addHeaderCell(headerCell);
					}

					for (int i = 1; i < lines.size(); i++) {
						String[] values = lines.get(i).split(",");
						for (String value : values) {
							String[] rowCells = value.split(":");
							Cell rowCell = new Cell().add(new Paragraph(rowCells[0]))
									// .setBackgroundColor(new DeviceRgb(221, 221, 221)) // light grey background
									// .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
									.setBorder(new SolidBorder(2)).setBorderRight(new SolidBorder(1));
							table.addCell(rowCell);
							if (rowCells.length > 1) {
								Cell rowCell1 = new Cell().add(new Paragraph(rowCells[1]))
										// .setBackgroundColor(new DeviceRgb(221, 221, 221)) // light grey background
										// .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
										.setBorder(new SolidBorder(2)).setBorderRight(new SolidBorder(1));

								table.addCell(rowCell1);
							}
						}
					}

					document.add(table);
				}

				document.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<Path> listFiles(String directoryPath) throws IOException {
		List<Path> filePaths = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath))) {
			for (Path path : directoryStream) {
				if (Files.isRegularFile(path)) {
					filePaths.add(path);
				}
			}
		}
		return filePaths;
	}
}
