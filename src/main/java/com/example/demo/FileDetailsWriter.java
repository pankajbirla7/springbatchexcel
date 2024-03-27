package com.example.demo;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;

public class FileDetailsWriter {

	private final JdbcTemplate jdbcTemplate;

	public FileDetailsWriter(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	//@Override
	public void write(Chunk<? extends FileDetails> chunk) throws Exception {
		System.out.println("Total items to write in db : " + chunk.size());
		if (chunk != null && chunk.getItems().size() > 0) {
			for (FileDetails item : chunk.getItems()) {
				if(item.getAgencyFin()!=null) {
					System.out.println("Writing item: " + item);
					String sql = "INSERT INTO File (AgencyFEIN, Filename, Submitted_By_Email, SubmitDate) VALUES (?, ?, ?, CURRENT_DATE())";
					jdbcTemplate.update(sql, Integer.parseInt(item.getAgencyFin()), item.getFileName(),  item.getSubmittedByEmail());
				}else {
					continue;
				}
			}
		}
	}
}
