package com.example.repository;

import java.util.Date;
import java.util.List;

import com.example.dto.SafeNetClaim;
import com.example.dto.StdClaim;

public interface SafeNetClaimDao {

	int getClaimCountByDateEntered(Date dateEntered);

	List<SafeNetClaim> getClaimIds(Date dateEntered);

}
