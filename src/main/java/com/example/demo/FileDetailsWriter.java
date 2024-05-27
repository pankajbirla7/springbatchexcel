package com.example.demo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

public class FileDetailsWriter {

	private final JdbcTemplate jdbcTemplate;

	public FileDetailsWriter(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	// @Override
	public int write(Chunk<? extends FileDetails> chunk) throws Exception {
		System.out.println("Total items to write in db : " + chunk.size());
		if (chunk != null && chunk.getItems().size() > 0) {
			for (FileDetails item : chunk.getItems()) {
				if (item.getAgencyFein() != null) {
					System.out.println("Writing item: " + item);
					String sql = "INSERT INTO Files (AgencyFEIN, Filename, Submitted_By_Email, SubmitDate) VALUES (?, ?, ?, CURRENT_DATE())";
					// int i = jdbcTemplate.update(sql, Integer.parseInt(item.getAgencyFin()),
					// item.getFileName(), item.getSubmittedByEmail());
					KeyHolder keyHolder = new GeneratedKeyHolder();

					jdbcTemplate.update(new PreparedStatementCreator() {
						@Override
						public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
							PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
							ps.setInt(1, Integer.parseInt(item.getAgencyFein()));
							ps.setString(2, item.getFileName());
							ps.setString(3, item.getSubmittedByEmail());
							return ps;
						}
					}, keyHolder);

					// Retrieve the generated ID
					int generatedId = keyHolder.getKey().intValue();
					return generatedId;
				} else {
					continue;
				}
			}
		}
		return 0;
	}
}
