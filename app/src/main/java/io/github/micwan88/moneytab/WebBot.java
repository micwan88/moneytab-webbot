package io.github.micwan88.moneytab;

import java.io.Closeable;
import java.io.File;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.micwan88.helperclass4j.AppPropertiesUtil;

public class WebBot implements Closeable {
	
	private static final Logger myLogger = LogManager.getLogger(WebBot.class);
	
	private WebDriver webDriver = null;
	
	private boolean browserHeadlessMode = false;
	private boolean browserDetachWhenErr = false;
	private File browserUserData = null;
	
	public int loadAppParameters(Properties appProperties) {
		String tempStr = appProperties.getProperty("moneytab.bot.browserHeadlessMode");
		if (tempStr != null && tempStr.trim().equalsIgnoreCase("true")) {
			browserHeadlessMode = true;
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.browserDetachWhenError");
		if (tempStr != null && tempStr.trim().equalsIgnoreCase("true")) {
			browserDetachWhenErr = true;
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.browserUserData");
		if (tempStr != null && !tempStr.trim().equals("")) {
			browserUserData = new File(tempStr);
			
			if (!browserUserData.isDirectory() || !browserUserData.exists()) {
				myLogger.error("Browser user data not exist {}: {}", "moneytab.bot.browserUserData", browserUserData.getAbsolutePath());
				return -1;
			}
		}
		
		return 0;
	}
	
	public void debugParams() {
		myLogger.debug("Debug Params ...");
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.browserHeadlessMode", browserHeadlessMode);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.browserDetachWhenError", browserDetachWhenErr);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.browserUserData", browserUserData != null ? browserUserData.getAbsolutePath() : null);
	}
	
	public static void main(String[] args) {
		myLogger.debug("Loading appProperties ...");
		AppPropertiesUtil appPropertyUtil = new AppPropertiesUtil();
		Properties appProperties = appPropertyUtil.getAppProperty();
		if (appProperties == null) {
			myLogger.error("Cannot load appProperties: {}", AppPropertiesUtil.APP_PROPERTY_FILE);
			System.exit(-1);
		}
		
		WebBot webBot = new WebBot();
		
		int returnCode = webBot.loadAppParameters(appProperties);
		
		webBot.debugParams();
		
		if (returnCode != 0) {
			myLogger.error("Invalid parameters");
			System.exit(-2);
		}
		
		try {
			webBot.init();
			
			//returnCode = webBot.startProcess();
		} finally {
			webBot.close();
		}
		
		if (returnCode != 0)
			System.exit(-3);
	}
	
	public void init() {
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.setHeadless(browserHeadlessMode);
		if (browserUserData != null)
			chromeOptions.addArguments("user-data-dir=" + browserUserData.getAbsolutePath());
		chromeOptions.addArguments("--remote-debugging-port=9222");
		
		webDriver = new ChromeDriver(chromeOptions);
	}

	@Override
	public void close() {
		if (webDriver != null)
			webDriver.quit();
	}
	
	public String loadMoneyTabWeb() {
		myLogger.debug("startProcess ...");
		
		webDriver.get("https://www.money-tab.com");
		
		myLogger.debug("startProcess end");
		return "";
	}
}
