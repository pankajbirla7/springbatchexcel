package com.example.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TextFileParser {
    public static void main(String[] args) {
    	
    	List<String> d = new ArrayList<>();
    	d.add("ABC");
    	d.add("XYZ");
    	
    	System.out.println("String list : "+String.valueOf(d));
    	
        String filePath = "D:\\Projects\\Ronak\\decryptedfiles\\t.txt";
        Map<String, String> voucherDetailsAndStatusMap = new HashMap<>();
        try {
            File file = new File(filePath);
            Scanner scanner = new Scanner(file);

            // Skip the first two lines (header rows)
            scanner.nextLine(); // Skip first header row
            scanner.nextLine(); // Skip second header row

            while (scanner.hasNextLine()) {
                String dataLine = scanner.nextLine();
                String[] values = dataLine.trim().split("\\s+");

                for (String value : values) {
                    System.out.print(value + " ");
                    String docNumber = values[3];
                    String error = values[7];
                    if(docNumber!=null && docNumber.startsWith("1S")) {
                    	if(voucherDetailsAndStatusMap.get(docNumber)!=null) { 
                    		if(!voucherDetailsAndStatusMap.get(docNumber).equals("SUCCESS") && !voucherDetailsAndStatusMap.get(docNumber).equals("ERROR")) {
                    			voucherDetailsAndStatusMap.put(docNumber, error);
                    		}
                    	}else {
                    		voucherDetailsAndStatusMap.put(docNumber, error);
                    	}
                    }
                }
                System.out.println();
            }

            scanner.close();

        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Voucher Details Map : "+voucherDetailsAndStatusMap);
        
        for (Map.Entry<String, String> set :
        	voucherDetailsAndStatusMap.entrySet()) {

           // Printing all elements of a Map
           System.out.println(set.getKey() + " = "
                              + set.getValue() + "  ==== "+getClaimIdFromVoucher(set.getKey())); 
           
       }
    }
    
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
