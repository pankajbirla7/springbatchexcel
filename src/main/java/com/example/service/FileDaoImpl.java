package com.example.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.FileDetails;

@Repository
public class FileDaoImpl implements FileDao {
	
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Override
	public FileDetails getFileDetailsByFileID(int fileid) {
		FileDetails fileDetails = new FileDetails();
		
		String sql = "SELECT * FROM files where id = ?";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[]{fileid});

		for (Map<String, Object> row : rows) {
			int id = (int) row.get("id");
			int agencyFin = (int) row.get("AgencyFEIN");
			Date rawLoadDate = (Date) row.get("RawLoadDate");
			fileDetails.setRawLoadDate(rawLoadDate);
			fileDetails.setId(Long.valueOf(id));
			fileDetails.setAgencyFein(String.valueOf(agencyFin));
		}
		return fileDetails;
	}

	@Override
	public FileDetails getFileDetailsByFeinNumber(String feinNumber) {
		FileDetails fileDetails = new FileDetails();
		
		String sql = "SELECT * FROM files where AgencyFEIN = ?";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[]{feinNumber});

		for (Map<String, Object> row : rows) {
			int id = (int) row.get("id");
			int agencyFin = (int) row.get("AgencyFEIN");
			Date rawLoadDate = (Date) row.get("RawLoadDate");
			String submittedByEmail = (String) row.get("Submitted_By_Email");
			fileDetails.setRawLoadDate(rawLoadDate);
			fileDetails.setId(Long.valueOf(id));
			fileDetails.setAgencyFein(String.valueOf(agencyFin));
			fileDetails.setSubmittedByEmail(submittedByEmail);
		}
		return fileDetails;
	}

}
