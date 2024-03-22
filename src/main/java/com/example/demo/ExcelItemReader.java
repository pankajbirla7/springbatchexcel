	package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.ItemReader;

public class ExcelItemReader implements ItemReader<MyDataObject> {

	private Iterator<Row> rowIterator;
	private boolean firstRowSkipped = false;

	public ExcelItemReader() {
		FileInputStream file = null;
		try {
			file = new FileInputStream(new File("C:\\Users\\acer\\Downloads\\test.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);

			XSSFSheet sheet = workbook.getSheetAt(0);
			rowIterator = sheet.iterator();
			rowIterator.hasNext();
			rowIterator.next();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public MyDataObject read() {
		if (rowIterator != null && rowIterator.hasNext()) {
			MyDataObject dataObject = new MyDataObject();
			Row row = rowIterator.next();
			Iterator<Cell> cellIterator = row.cellIterator();
			int colCount = -1;
			Map<String, String> rowMap = new HashMap<>();
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				colCount++;
				switch (cell.getCellType()) {
				case NUMERIC:
					int intValue = (int) cell.getNumericCellValue();
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
			System.out.println();
			if (!firstRowSkipped) {
				firstRowSkipped = true;
			}
			if(rowMap.size()>0) {
				dataObject.setFin(rowMap.get(0+""));
				dataObject.setCin(rowMap.get(1+""));
				dataObject.setNdc(rowMap.get(3+""));
				String rowString = rowMap.get(2+"");
		        SimpleDateFormat inputFormat = new SimpleDateFormat("MMddyyyy");
		        SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd-yyyy");
		        String formattedDateString = "";
		        try {
		            Date date = inputFormat.parse(rowString);
		            formattedDateString = outputFormat.format(date);
		            System.out.println(formattedDateString);
		        } catch (ParseException e) {
		            e.printStackTrace();
		        }
				dataObject.setDateEntered(formattedDateString);
			}else {
				return null;
			}
			return dataObject;
		}
		return null;
	}

	// @Override
	public MyDataObject readss() {
		if (rowIterator != null && rowIterator.hasNext()) {
			if (!firstRowSkipped) {
				// Skip the first row
				firstRowSkipped = true;
				rowIterator.hasNext();
			}
			Row rs = rowIterator.next();

			MyDataObject dataObject = new MyDataObject();
			Cell finCell = rs.getCell(0);
			if (finCell.getCellType() == CellType.NUMERIC) {
				dataObject.setFin(finCell.getNumericCellValue() + "");
			} else if (finCell.getCellType().equals(CellType.STRING)) {
				dataObject.setFin(finCell.getStringCellValue());
			}

			Cell cinCell = rs.getCell(1);
			if (cinCell.getCellType() == CellType.NUMERIC) {
				dataObject.setCin(cinCell.getNumericCellValue() + "");
			} else if (finCell.getCellType().equals(CellType.STRING)) {
				dataObject.setCin(cinCell.getStringCellValue());
			}

			Cell ndcCell = rs.getCell(3);
			if (ndcCell.getCellType() == CellType.NUMERIC) {
				dataObject.setNdc(ndcCell.getNumericCellValue() + "");
			} else if (finCell.getCellType().equals(CellType.STRING)) {
				dataObject.setCin(cinCell.getStringCellValue());
			}

			Cell dateCell = rs.getCell(2);
			String rowString = dateCell.getStringCellValue();
			SimpleDateFormat inputFormat = new SimpleDateFormat("MMddyyyy");
			SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd-yyyy");
			String formattedDateString = "";
			try {
				Date date = inputFormat.parse(rowString);
				formattedDateString = outputFormat.format(date);
				System.out.println(formattedDateString);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			dataObject.setDateEntered(formattedDateString);

			return dataObject;
		}
		return null;
	}
}
