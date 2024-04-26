package com.example.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.dto.SafeNetClaim;
import com.example.dto.StdClaim;

@Repository
public class StdClaimDaoImpl implements StdClaimDao {

	@Autowired
	public JdbcTemplate jdbcTemplate;

	@Override
	public List<StdClaim> getStdClaimDetails(int processed) {
		List<StdClaim> stdClaimList = new ArrayList<>();

		String sql = "SELECT distinct File_id FROM std_claims where DateToSFS is null and status_cd = ?";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[]{processed});

		for (Map<String, Object> row : rows) {
			StdClaim stdClaim = new StdClaim();
			int fileId = (int) row.get("File_id");
			stdClaim.setFileid(fileId);

			stdClaimList.add(stdClaim);
		}
		return stdClaimList;
	}
	
	@Override
	public int getClaimCountByDateEntered(Date dateEntered, int status) {
		String sql = "select count(distinct id) as claimCount from std_claims \n"
				+ " where DateEntered > ? \n"
				+ " and DateToSFS is null and status_cd = "+status;
		int claimCount = jdbcTemplate.queryForObject(sql, new Object[]{dateEntered}, Integer.class);
		return claimCount;
	}

	@Override
	public List<StdClaim> getClaimIds(Date dateEntered, int status) {
		List<StdClaim> stdClaims = new ArrayList<>();

		String sql = "SELECT distinct id from std_claims \n"
				+ "	where DateEntered > ? \n"
				+ "	and DateToSFS is null and status_cd = "+status+" order by id asc";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[]{dateEntered});

		for (Map<String, Object> row : rows) {
			StdClaim stdClaim = new StdClaim();
			int claimId = (int) row.get("id");
			stdClaim.setId(claimId);

			stdClaims.add(stdClaim);
		}
		return stdClaims;
	}

}
