package com.example.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.dto.StdClaim;
import com.example.utility.Utility;

@Repository
public class StdClaimDaoImpl implements StdClaimDao {

	static final Logger logger = LoggerFactory.getLogger(StdClaimDaoImpl.class);
	
	@Autowired
	public JdbcTemplate jdbcTemplate;

	@Override
	public List<StdClaim> getStdClaimDetails(int processed) {
		List<StdClaim> stdClaimList = new ArrayList<>();
		try {
			String sql = "SELECT distinct File_id FROM std_claims where DateToSFS is null and status_cd = ?";
	
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[] { processed });
			logger.info("Query executed to find stdclaim details based on dateToSfs is null and Status_CD as - "+processed+ 
					" :: Query - "+sql+ " :: result returned :: "+rows );
			for (Map<String, Object> row : rows) {
				StdClaim stdClaim = new StdClaim();
				int fileId = (int) row.get("File_id");
				stdClaim.setFileid(fileId);
	
				stdClaimList.add(stdClaim);
			}
		}catch(Exception e) {
			e.printStackTrace();
			logger.error("Exception occured while getting stdclaim details where dateToSfs is null and status_cd is :: "+processed+ " :: due to :: "+Utility.getStackTrace(e));
		}
		return stdClaimList;
	}

	@Override
	public int getClaimCountByDateEntered(Date dateEntered, int status) {
		String sql = "select count(distinct id) as claimCount from std_claims \n" + " where DateEntered > ? \n"
				+ " and DateToSFS is null and status_cd = " + status;
		int claimCount = jdbcTemplate.queryForObject(sql, new Object[] { dateEntered }, Integer.class);
		return claimCount;
	}

	@Override
	public List<StdClaim> getClaimIds(Date dateEntered, int status) {
		List<StdClaim> stdClaims = new ArrayList<>();

		String sql = "SELECT distinct id from std_claims \n" + "	where DateEntered > ? \n"
				+ "	and DateToSFS is null and status_cd = " + status + " order by id asc";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[] { dateEntered });

		for (Map<String, Object> row : rows) {
			StdClaim stdClaim = new StdClaim();
			int claimId = (int) row.get("id");
			stdClaim.setId(claimId);

			stdClaims.add(stdClaim);
		}
		return stdClaims;
	}

	@Override
	public void updateStandardDetailSfsDate(List<StdClaim> stdList) {
		StringBuilder idList = new StringBuilder();
		for (StdClaim stdClaim : stdList) {
			idList.append(stdClaim.getId()).append(",");
		}
		idList.deleteCharAt(idList.length() - 1); // Remove the last comma

		// Construct the SQL query
		String updateQuery = "UPDATE std_claims SET DateToSFS = CURDATE() WHERE id IN (" + idList.toString() + ")";

		jdbcTemplate.update(updateQuery);

	}

	@Override
	public void updateVoucherDetailsAndStatus(int claimId, String voucher, int error) {
		StringBuilder updateQuery = new StringBuilder("UPDATE std_claims SET ");
		updateQuery.append("VoucherId = '").append(voucher).append("', ");
		updateQuery.append("status_cd = '").append(error).append("' ");
		updateQuery.append("WHERE ID = ").append(claimId).append(";");

		// Execute the update query
		jdbcTemplate.batchUpdate(updateQuery.toString());
		
	}

}
