package com.example.demo;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

public class ExcelItemWriter implements ItemWriter<MyDataObject> {

	private final JdbcTemplate jdbcTemplate;

	public ExcelItemWriter(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void write(Chunk<? extends MyDataObject> chunk) throws Exception {
		System.out.println("Total items to write in db : " + chunk.size());
		if (chunk != null && chunk.getItems().size() > 0) {
			for (MyDataObject item : chunk.getItems()) {
				if(item.getFin()!=null) {
					System.out.println("Writing item: " + item);
					String sql = "INSERT INTO Raw_Claims (FIN, CIN, dfilled, NDC, DateEntered) VALUES (?, ?, ?, ?, CURRENT_DATE())";
					jdbcTemplate.update(sql, item.getFin(), item.getCin(), item.getDfilled(), item.getNdc());
				}else {
					continue;
				}
			}
		}
	}
}
