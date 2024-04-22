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
public class SafeNetClaimDaoImpl implements SafeNetClaimDao {

	@Autowired
	public JdbcTemplate jdbcTemplate;
	
	@Override
	public int getClaimCountByDateEntered(Date dateEntered) {
		String sql = "select count(distinct claimId) as claimCount from safenet_Claims \n"
				+ " where DateEntered > ? \n"
				+ " and DateToSFS is null and statuscd is null";
		int claimCount = jdbcTemplate.queryForObject(sql, new Object[]{dateEntered}, Integer.class);
		return claimCount;
	}

	@Override
	public List<SafeNetClaim> getClaimIds(Date dateEntered) {
		List<SafeNetClaim> safeNetClaims = new ArrayList<>();

		String sql = "SELECT distinct claimId from safenet_Claims \n"
				+ "	where DateEntered > ? \n"
				+ "	and DateToSFS is null and statuscd is null order by ClaimId";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[]{dateEntered});

		for (Map<String, Object> row : rows) {
			SafeNetClaim safeNetClaim = new SafeNetClaim();
			//int id = (Integer) row.get("ID");
			int claimId = (int) row.get("claimId");
		//	Date dateEnt = (Date) row.get("DateEntered");
		//	Date datToSfs = (Date) row.get("DateToSFS");
		//	String statuscd = (String) row.get("statuscd");
			
			//safeNetClaim.setId(id);
			safeNetClaim.setClaimId(claimId);
		///	safeNetClaim.setDateEntered(dateEnt);
		//	safeNetClaim.setDateToSfs(datToSfs);
		//	safeNetClaim.setStatuscd(statuscd);

			safeNetClaims.add(safeNetClaim);
		}
		return safeNetClaims;
	}

}
