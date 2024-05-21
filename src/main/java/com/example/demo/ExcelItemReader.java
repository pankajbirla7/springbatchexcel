package com.example.demo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

public class ExcelItemReader implements ItemReader {

	private Iterator<Row> rowIterator;
	private Workbook workbook;
	private boolean firstRowSkipped = false;
	private int resourceIndex;
	private Resource[] resources = null;
	private ExcelItemWriter itemWriter;
	private FileDetailsWriter fileDetailsWriter;
	private final JdbcTemplate jdbcTemplate;
	private Map<String, Short> rowStatusMap = null;

	@Value("${input.file.paths}")
	private String inputFiles;

	@Value("${output.file.path}")
	private String outputFileDirectory;

	@Autowired
	private DataSource dataSource;

	public ExcelItemReader(ExcelItemWriter excelItemWriter, FileDetailsWriter fileDetailsWriter,
			DataSource dataSource) {
		try {
			this.resourceIndex = 0;
			this.itemWriter = excelItemWriter;
			this.fileDetailsWriter = fileDetailsWriter;
			this.outputFileDirectory = outputFileDirectory;
			this.jdbcTemplate = new JdbcTemplate(dataSource);
			rowStatusMap = getRawStatusMap();
			// openWorkbook();
		} catch (Exception e) {
			throw new RuntimeException("Error opening Excel file", e);
		}
	}

	public Resource[] inputFiles() {
		try {
			ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
			Resource[] resources = resourcePatternResolver.getResources("file:" + inputFiles + "/*.xlsx");
			return resources;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void openWorkbook() {
		if (resourceIndex < resources.length) {
			try (InputStream inputStream = resources[resourceIndex].getInputStream()) {
				this.workbook = new XSSFWorkbook(inputStream);
				this.rowIterator = workbook.getSheetAt(0).iterator();
				rowIterator.hasNext();
				rowIterator.next();
			} catch (Exception e) {
				throw new RuntimeException("Error opening Excel file", e);
			}
		} else {
			closeWorkbook();
			this.workbook = null;
			this.rowIterator = null;
		}
	}

	@Override
	public MyDataObject read() {
		resources = inputFiles();
		for (Resource resource : resources) {
			try (InputStream inputStream = resource.getInputStream()) {
				this.workbook = new XSSFWorkbook(inputStream);
				this.rowIterator = workbook.getSheetAt(0).iterator();
				rowIterator.hasNext();
				rowIterator.next();
				processFile(rowIterator, resource);

				closeWorkbook();
			} catch (Exception e) {
				e.printStackTrace();
				try {
					EmailUtility.sendEmail("File processing failed for file " + resource.getFile().getAbsolutePath(),
							Constants.FAILED);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				throw new RuntimeException("Error opening Excel file", e);
			}
		}
		return null;
	}

	private void processFile(Iterator<Row> rowIterator, Resource resource) throws Exception {
		List<MyDataObject> items = new ArrayList<>();
		int fileId = 0;
		while (rowIterator != null && rowIterator.hasNext()) {
			MyDataObject dataObject = new MyDataObject();
			Row row = rowIterator.next();
			if (isRowEmpty(row)) {
				break; // Skip empty rows
			}
			Iterator<Cell> cellIterator = row.cellIterator();
			int colCount = -1;
			Map<String, String> rowMap = new HashMap<>();
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				if (colCount >= 3) {
					break;
				}
				colCount++;
				if (cell.getCellType() == CellType.NUMERIC
						&& org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {

				} else {
					if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.STRING) {
						cell.setCellType(CellType.STRING);
					}
				}

				switch (cell.getCellType()) {
				case NUMERIC:
					String cellValue = "";
					if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
						DataFormatter formatter = new DataFormatter();
						cellValue = formatter.formatCellValue(cell);
						DateFormat dateFormatter = new SimpleDateFormat("MMddyyyy");
						Date date = cell.getDateCellValue();
						String dateStr1 = dateFormatter.format(date);
						if (date.getYear() + "".length() != 4) {
							String dateValue = cellValue.replaceAll("/", "");
							dateValue = dateValue.replaceAll("-", "");
							if (dateValue.length() != 8 && dateStr1.length() != 8 && !dateStr1.contains("/")
									&& !dateStr1.contains("-")) {
								cell.setCellType(CellType.STRING);
								cellValue = cell.getStringCellValue();
							} else {
								String dateStr = dateFormatter.format(date);
								cellValue = dateStr;
							}
						} else {
							String dateStr = dateFormatter.format(date);
							cellValue = dateStr;
						}
					} else {
						cellValue = String.valueOf(cell.getNumericCellValue());
					}
					String intValue = cellValue;
					System.out.print(intValue + "                 ");
					rowMap.put(colCount + "", intValue + "");
					break;
				case STRING:
					String value = cell.getStringCellValue();
					System.out.print(value + "                 ");
					rowMap.put(colCount + "", value);
					break;
				}
			}

			if (!firstRowSkipped) {
				firstRowSkipped = true;
			}
			if (rowMap.size() > 0) {
				dataObject.setFein(rowMap.get(0 + ""));
				dataObject.setCin(rowMap.get(1 + ""));
				dataObject.setDfilled(rowMap.get(2 + ""));
				dataObject.setNdc(rowMap.get(3 + ""));
				dataObject.setStatusCode(rowStatusMap.get("New"));
				if (fileId == 0) {
					fileId = insertFileDetail(resource, dataObject.getFein());
				}
				dataObject.setFileId(fileId);

				items.add(dataObject);
			}

			System.out.println();
		}

		if (items != null && items.size() > 0) {
			try {

				Chunk<MyDataObject> fileWriterChunk = new Chunk<>(items);
				itemWriter.write(fileWriterChunk);

				moveFilesToArchiveFolder(items.get(0).getFein(), resource);
			} catch (Exception e) {

				try {
					EmailUtility.sendEmail("File moving to archive failed" + resource.getFile().getAbsolutePath(),
							Constants.FAILED);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		try {
			EmailUtility.sendEmail("File processing success" + resource.getFile().getAbsolutePath(),
					Constants.SUCCESSS);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private int insertFileDetail(Resource resource, String agencyFein) throws Exception {
		List<FileDetails> fileDetailsList = new ArrayList<>();
		FileDetails fileDetails = new FileDetails();
		fileDetails.setAgencyFein(agencyFein);
		fileDetails.setFileName(resource.getFilename());
		fileDetails.setSubmittedByEmail("abc@gmail.com");
		java.sql.Date d = java.sql.Date.valueOf("2024-03-26");
		fileDetails.setSubmitDate(d);
		fileDetailsList.add(fileDetails);

		Chunk<FileDetails> fileDetailsChunk = new Chunk<>(fileDetailsList);
		int fileId = fileDetailsWriter.write(fileDetailsChunk);

		return fileId;
	}

	private void moveFilesToArchiveFolder(String fin, Resource resource) {

		String agencyName = getAgencyName(fin);
		System.out.println("Agency Name = " + agencyName);
		if (agencyName != null) {
			try {
				// Get the file path from the resource object
				Path source = Paths.get(resource.getFile().getAbsolutePath());

				// Define destination folder path
				Path destinationFolder = Paths.get(outputFileDirectory + "\\" + agencyName + " - " + fin);

				if (!Files.exists(destinationFolder)) {
					Files.createDirectories(destinationFolder);
				}

				// Define destination file path
				Path destination = destinationFolder.resolve(source.getFileName());

				// Move the file
				Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("File moved successfully from " + source + " to " + destination);
			} catch (Exception e) {
				System.err.println("Error moving the file: " + e.getMessage());
				try {
					EmailUtility.sendEmail(
							"File moving to archive folder got failed" + resource.getFile().getAbsolutePath(),
							Constants.FAILED);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				e.printStackTrace();
			}
		} else {
			System.out.println("Agency Name Not found");
		}
	}

	private String getAgencyName(String fin) {
		try {
			String sql = "SELECT * from agency where FEIN = ?";
			return jdbcTemplate.queryForObject(sql, new Object[] { Integer.parseInt(fin) },
					(rs, rowNum) -> new String(rs.getString("Agency Name")));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private int getRawStatusCodeByStatus(String status) {
		try {
			String sql = "SELECT * from raw_status where Description = ?";
			return jdbcTemplate.queryForObject(sql, new Object[] { status }, (rs, rowNum) -> rs.getInt("Code"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	public Map<String, Short> getRawStatusMap() {
		String sql = "SELECT Code, Description FROM raw_status";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

		Map<String, Short> statusMap = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Short code = ((Number) row.get("Code")).shortValue();
			String status = (String) row.get("Description");
			statusMap.put(status, code);
		}

		return statusMap;
	}

	private boolean isRowEmpty(Row row) {
		if (row == null) {
			return true;
		}
		for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
			Cell cell = row.getCell(i);
			if (cell != null && cell.getCellType() != CellType.BLANK) {
				return false;
			}
		}
		return true;
	}

	private void closeWorkbook() {
		try {
			if (workbook != null) {
				workbook.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("Error closing Excel workbook", e);
		}
	}
}