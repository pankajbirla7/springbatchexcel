package com.example.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtils {
	 public static int getClaimIdFromVoucher(String voucher) {
	        int claimId = 0;        
	        // Define the regular expression pattern
	        Pattern pattern = Pattern.compile("^1S0*([1-9][0-9]*)");

	        // Match the pattern against the input string
	        Matcher matcher = pattern.matcher(voucher);

	        // Check if the pattern matches
	        if (matcher.find()) {
	            // Extract the matched group
	            String extracted = matcher.group(1);
	            claimId = Integer.parseInt(extracted);
	        }
			return claimId;
	    }
}
