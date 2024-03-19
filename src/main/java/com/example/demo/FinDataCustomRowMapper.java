package com.example.demo;

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

        MyDataObject dataObject = new MyDataObject();
        dataObject.setFin(rs.getCurrentRow()[0]);
        dataObject.setCin(rs.getCurrentRow()[1]);
        dataObject.setDateEntered(rs.getCurrentRow()[2]);
        dataObject.setNdc(rs.getCurrentRow()[3]);
        return dataObject;
        
    }
}
