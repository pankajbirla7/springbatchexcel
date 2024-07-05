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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.example.demo.Constants;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public class CsvToPdf {
	public static void main(String[] args) throws Exception {
		String csvFile = "D:\\Projects\\Ronak\\csvfiles\\";
		String pdfFile = "D:\\Projects\\Ronak\\pdffiles\\";

		CsvToPdf csvToPdf = new CsvToPdf();
		csvToPdf.migaretCsvToPdfFiles1(csvFile, pdfFile);
		// csvToPdf.migaretCsvToPdfFiles(csvFile, pdfFile);

//		String htmlFilePath = "D:\\Projects\\Ronak\\htmlfiles\\myhtml.html";
//		csvToPdf.processHtmlFile(htmlFilePath, pdfFile);
//		csvToPdf.createPdfFileManually(pdfFile);

	//	csvToPdf.createPdfFileByDynamicData(csvFile, pdfFile);
	}
	
	public void migaretCsvToPdfFiles1(String csvFilePath, String pdfFilePath) throws Exception {
		try {
			List<Path> filePaths = listFiles(csvFilePath);
			for (Path filePath : filePaths) {
				Map<String, String> data = new LinkedHashMap<>();
				File file = filePath.toFile();
				String[] csvFileNameArr = file.getName().split("_");
				String feinNumber = csvFileNameArr[0];
				String pdfFile = searchAndCreateFile(pdfFilePath, feinNumber, FilenameUtils.removeExtension(file.getName()) + ".pdf");
				//String pdfFile = pdfFilePath + File.separator + FilenameUtils.removeExtension(file.getName()) + ".pdf";
				if(pdfFile == null) {
					//logger.info("Folder name with fein number not found to create pdf file from csv :: "+file.getAbsolutePath()+ " :: and fein number from csv file :: "+feinNumber);
					break;
				}
				PdfWriter writer = new PdfWriter(pdfFile);

				PdfDocument pdfDoc = new PdfDocument(writer);

				Document document = new Document(pdfDoc);

				Path path = Paths.get(file.getAbsolutePath());
				List<String> lines = Files.readAllLines(path);

				if (!lines.isEmpty()) {
					String[] headers = lines.get(0).split(",");

					for (int i = 1; i < lines.size(); i++) {
						String[] values = lines.get(i).split(":");
						if (values.length > 1) {
							data.put(values[0], values[1]);
						} else {
							// data.put(values[0], "");
						}
					}

					Color borderColor = new DeviceRgb(0, 0, 0); // Black color

					Table table = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();

					Cell headerCell = new Cell(1, 2).add(new Paragraph(headers[0]))
							.setBackgroundColor(new DeviceRgb(221, 221, 221))
							.setTextAlignment(TextAlignment.CENTER).setBold();
					table.addHeaderCell(headerCell);

					for (Map.Entry<String, String> entry : data.entrySet()) {
						Cell keyCell = new Cell().add(new Paragraph(entry.getKey())).setBold()
								.setBorder(new SolidBorder(borderColor, 2));
						table.addCell(keyCell);

						Cell valueCell = new Cell().add(new Paragraph(entry.getValue()))
								.setBorder(new SolidBorder(borderColor, 2));
						table.addCell(valueCell);
					}

					document.add(table);

					document.close();

				//	logger.info("PDF created successfully! - " + pdfFile + " for csv filepath : "+file.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public static String searchAndCreateFile(String rootDirectoryPath, String folderNameToFind, String fileNameToCreate) {
        File rootDirectory = new File(rootDirectoryPath);

        if (rootDirectory.exists() && rootDirectory.isDirectory()) {
            File[] subDirectories = rootDirectory.listFiles(File::isDirectory);

            if (subDirectories != null) {
                for (File folder : subDirectories) {
                    if (folder.getName().contains(folderNameToFind)) {
                        File newFile = new File(folder, fileNameToCreate);
                        return newFile.getAbsolutePath();
                    }
                }
            }
        }

        return null;
    }

	private void createPdfFileByDynamicData(String csvFilePath, String pdfFilePath) {
		try {
			List<Path> filePaths = listFiles(csvFilePath);
			for (Path filePath : filePaths) {
				Map<String, String> data = new LinkedHashMap<>();
				File file = filePath.toFile();
				PdfWriter writer = new PdfWriter(
						pdfFilePath + File.separator + FilenameUtils.removeExtension(file.getName()) + ".pdf");

				PdfDocument pdfDoc = new PdfDocument(writer);

				Document document = new Document(pdfDoc);

				Path path = Paths.get(file.getAbsolutePath());
				List<String> lines = Files.readAllLines(path);

				if (!lines.isEmpty()) {
					String[] headers = lines.get(0).split(",");
//					Table table = new Table(headers.length);
//
//					for (String header : headers) {
//						Cell headerCell = new Cell().add(new Paragraph(header).setBold())
//								.setBackgroundColor(new DeviceRgb(221, 221, 221)) // light grey background
//								.setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
//								.setBorder(new SolidBorder(1));
//						table.addHeaderCell(headerCell);
//					}

					for (int i = 1; i < lines.size(); i++) {
						String[] values = lines.get(i).split(":");
						if (values.length > 1) {
							data.put(values[0], values[1]);
						} else {
						//	data.put(values[0], "");
						}
					}

					Color borderColor = new DeviceRgb(0, 0, 0); // Black color

					Table table = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();

					Cell headerCell = new Cell(1, 2).add(new Paragraph(headers[0]))
							.setTextAlignment(TextAlignment.CENTER).setBold();
					table.addHeaderCell(headerCell);

					for (Map.Entry<String, String> entry : data.entrySet()) {
						Cell keyCell = new Cell().add(new Paragraph(entry.getKey())).setBold()
								.setBorder(new SolidBorder(borderColor, 2));
						table.addCell(keyCell);

						Cell valueCell = new Cell().add(new Paragraph(entry.getValue()))
								.setBorder(new SolidBorder(borderColor, 2));
						table.addCell(valueCell);
					}

					document.add(table);

					document.close();

					System.out.println("PDF created successfully!");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createPdfFileManually(String pdfFile) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("Voucher Date Range", "04/02/2024-04/30/2024");
		data.put("Date Created", "2024-06-25");
		data.put("Filename Submitted", "Alliance April 2024_340B Reinvestment_Voucher.xlsx");
		data.put("# Claims Submitted", "1174");
		data.put("# Claims Paid", "0");
		data.put("Reimbursement Rate Per Claim", "$505.00");
		data.put("Total Amount Reimbursed", "0.00");
		data.put("Date Sent To SFS", "2024-05-24");
		data.put("# Claims Rejected", "1174");
		data.put("Rejected Reason", "New;Error_in_SFS");

		// Define the output PDF file path
		String dest = pdfFile + "output_border.pdf";
		Color borderColor = new DeviceRgb(0, 0, 0); // Black color
		try {
			// Initialize PDF writer and document
			PdfWriter writer = new PdfWriter(dest);
			PdfDocument pdf = new PdfDocument(writer);
			Document document = new Document(pdf);

			// Create a table with 2 columns
			Table table = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();

			// Add header
			Cell headerCell = new Cell(1, 2).add(new Paragraph("AIDS Institute Safety Net 340B Payment Voucher"))
					.setTextAlignment(TextAlignment.CENTER).setBold();
			// .setBackgroundColor(ColorConstants.LIGHT_GRAY);
			table.addHeaderCell(headerCell);

			// Add key-value pairs to the table
			for (Map.Entry<String, String> entry : data.entrySet()) {
				// Add key cell
				Cell keyCell = new Cell().add(new Paragraph(entry.getKey())).setBold()
						.setBorder(new SolidBorder(borderColor, 2));
				table.addCell(keyCell);

				// Add value cell
				Cell valueCell = new Cell().add(new Paragraph(entry.getValue()))
						.setBorder(new SolidBorder(borderColor, 2));
				table.addCell(valueCell);
			}

			// Add the table to the document
			document.add(table);

			// Close the document
			document.close();

			System.out.println("PDF created successfully!");
		} catch (Exception e) {
			e.printStackTrace();
		}

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
