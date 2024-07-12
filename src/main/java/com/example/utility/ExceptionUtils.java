package com.example.utility;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ExceptionUtils {
	public static String getStackTrace(Exception ex) {
		Writer buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		ex.printStackTrace(pw);
		return buffer.toString();
	}
}
