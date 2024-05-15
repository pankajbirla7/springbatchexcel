package com.example.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Constants;
import com.example.repository.StdClaimDao;
import com.example.utility.AppUtils;
import com.example.utility.Utility;

@Service
public class StdClaimServiceImpl implements StdClaimService{

	@Autowired
	StdClaimDao stdClaimDao;
	
	@Override
	public void updateVoucherDetailsAndStatus(Map<String, String> voucherDetailsAndStatusMap) {
		
		int i =0;
		for (Map.Entry<String, String> set : voucherDetailsAndStatusMap.entrySet()) {
			int claimId = AppUtils.getClaimIdFromVoucher(set.getKey());
			String voucher = set.getKey();
			int error = set.getValue().equalsIgnoreCase("SUCCESS") ? Constants.SUCCESS : Constants.ERROR;
			
			stdClaimDao.updateVoucherDetailsAndStatus(claimId, voucher, error);
		}
		
		
	}

}
