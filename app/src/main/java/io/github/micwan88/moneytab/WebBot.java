package io.github.micwan88.moneytab;

import java.util.Properties;

import io.github.micwan88.helperclass4j.AppPropertiesUtil;

public class WebBot {
	public String dummyTest() {
		return "Test";
	}
	
	public static void main(String[] args) {
		Properties appProperties = AppPropertiesUtil.loadProperties(AppPropertiesUtil.APP_PROPERTY_FILE);
		if (appProperties == null) {
			
		}
	}
}
