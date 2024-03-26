package com.example.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.batch.extensions.excel.RowMapper;
import org.springframework.batch.extensions.excel.support.rowset.RowSet;

public class FinDataCustomRowMapper implements RowMapper<MyDataObject> {

    private boolean firstRowSkipped = false;

    @Override
    public MyDataObject mapRow(RowSet rs) throws Exception {
        if (!firstRowSkipped) {
            // Skip the first row
            firstRowSkipped = true;
            rs.next();
        }
        String rowString = rs.getCurrentRow()[2];
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

        MyDataObject dataObject = new MyDataObject();
        dataObject.setFin(rs.getCurrentRow()[0]);
        dataObject.setCin(rs.getCurrentRow()[1]);
        //dataObject.setDateEntered(formattedDateString);
        dataObject.setNdc(rs.getCurrentRow()[3]);
        return dataObject;
        
    }
}
