package com.example.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.FileDetails;
import com.example.dto.StdClaim;

@Repository
public class FileDaoImpl implements FileDao {
	
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Override
	public FileDetails getFileDetailsByFileID(int fileid) {
		FileDetails fileDetails = new FileDetails();
		
		String sql = "SELECT * FROM file where id = ?";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[]{fileid});

		for (Map<String, Object> row : rows) {
			Date submDate = (Date) row.get("SubmitDate");
			fileDetails.setSubmitDate(submDate);
		}
		return fileDetails;
	}

}
