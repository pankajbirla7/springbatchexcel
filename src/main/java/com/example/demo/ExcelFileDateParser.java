package com.example.demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelFileDateParser {
	public static void main(String[] args) {
        String excelFilePath = "path/to/your/excel/file.xlsx";
        String dbUrl = "jdbc:mysql://localhost:3306/yourdatabase";
        String dbUser = "yourusername";
        String dbPassword = "yourpassword";
        String dbTable = "yourtable";

        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            Sheet sheet = workbook.getSheetAt(0);
            DateFormat dateFormatter = new SimpleDateFormat("ddMMyyyy");
            DateFormat inputFormatter = new SimpleDateFormat("dd/MM/yyyy");

            String insertSQL = "INSERT INTO " + dbTable + " (date_column) VALUES (?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                for (Row row : sheet) {
                    Cell cell = row.getCell(0); // Assuming the date values are in the first column
                    String dateStr = "";

                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        if (cellValue.contains("/")) {
                            // Convert from dd/MM/yyyy to ddMMyyyy
                            try {
                                Date date = inputFormatter.parse(cellValue);
                                dateStr = dateFormatter.format(date);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Assume it's already in ddMMyyyy format
                            dateStr = cellValue;
                        }
                    } else if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        // Convert date to ddMMyyyy format
                        Date date = cell.getDateCellValue();
                        dateStr = dateFormatter.format(date);
                    }

                    pstmt.setString(1, dateStr);
                    pstmt.executeUpdate();
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
