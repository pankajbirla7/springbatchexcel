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
			file = new FileInputStream(new File("C:\\Users\\acer\\Documents\\test_new.xlsx"));
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
				if(colCount>=3) {
					break;
				}
				colCount++;
				if(cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.STRING) {
					cell.setCellType(CellType.STRING);
				}
				
				switch (cell.getCellType()) {
				case NUMERIC:
					String intValue = cell.getNumericCellValue()+"";
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
		            System.out.print(formattedDateString);
		        } catch (ParseException e) {
		            e.printStackTrace();
		        }
				dataObject.setDateEntered(formattedDateString);
			}else {
				return null;
			}
			System.out.println();
			return dataObject;
		}
		return null;
	}
}
