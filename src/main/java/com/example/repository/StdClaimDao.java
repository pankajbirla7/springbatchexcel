package com.example.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.example.dto.StdClaim;

public interface StdClaimDao {

	List<StdClaim> getStdClaimDetails(int pROCESSED);
	
	public int getClaimCountByDateEntered(Date dateEntered, int status);
	
	public List<StdClaim> getClaimIds(Date dateEntered, int status);

	void updateStandardDetailSfsDate(List<StdClaim> stdClaims2);

	void updateVoucherDetailsAndStatus(int claimId, String voucher, int error);

}
