package com.example.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.dto.StdClaim;

@Repository
public class StdClaimDaoImpl implements StdClaimDao {

	@Autowired
	public JdbcTemplate jdbcTemplate;

	@Override
	public List<StdClaim> getStdClaimDetails(int pROCESSED) {
		List<StdClaim> stdClaimList = new ArrayList<>();

		String sql = "SELECT * FROM std_claims";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

		for (Map<String, Object> row : rows) {
			StdClaim stdClaim = new StdClaim();
			int id = (Integer) row.get("ID");
			Date dateEntered = (Date) row.get("DateEntered");
			Date datToSfs = (Date) row.get("DateToSFS");
			int fileId = (int) row.get("File_id");
			stdClaim.setId(id);
			stdClaim.setDateEntered(dateEntered);
			stdClaim.setDateToSfs(datToSfs);
			stdClaim.setFileid(fileId);

			stdClaimList.add(stdClaim);
		}
		return stdClaimList;
	}
}
