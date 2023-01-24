package io.github.micwan88.moneytab.selenium;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

public class WebDriverMgr {
	
	private static final Logger myLogger = LogManager.getLogger(WebDriverMgr.class);
	
	public enum DRIVER_TYPE {
		CHROME,
		FIREFOX
	}
	
	public WebDriver getWebDriver(DRIVER_TYPE driverType, boolean isHeadlessMode, List<String> driverOptions) {
		if (driverType == DRIVER_TYPE.CHROME) {
			return getChromeDriver(isHeadlessMode, driverOptions);
		} else if (driverType == DRIVER_TYPE.FIREFOX) {
			return getFirefoxDriver(isHeadlessMode, driverOptions);
		}
		return null;
	}
	
	private WebDriver getFirefoxDriver(boolean isHeadlessMode, List<String> driverOptions) {
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		FirefoxOptions firefoxOptions = new FirefoxOptions();
		
		//Css 'has' selector for firefox need enable 'layout.css.has-selector.enabled'
		firefoxProfile.setPreference("layout.css.has-selector.enabled", true);
		firefoxProfile.layoutOnDisk();
		
		firefoxOptions.setProfile(firefoxProfile);
		
		if (isHeadlessMode)
			firefoxOptions.setHeadless(true);
		
		if (!driverOptions.isEmpty())
			firefoxOptions.addArguments(driverOptions);
		
		return new FirefoxDriver(firefoxOptions);
	}
	
	private WebDriver getChromeDriver(boolean isHeadlessMode, List<String> driverOptions) {
		ChromeOptions chromeOptions = new ChromeOptions();
		
		/**
		 * Not sure why we need this when in Linux, may be bug of chromedriver ?
		 * So hardcode it first ...
		 */
		chromeOptions.addArguments("--remote-debugging-port=9222");
		
		if (isHeadlessMode)
			chromeOptions.setHeadless(true);
		
		if (!driverOptions.isEmpty())
			chromeOptions.addArguments(driverOptions);
		
		return new ChromeDriver(chromeOptions);
	}
	
	public static void saveCookie(Path cookieFilePath, Set<Cookie> cookies) {
		myLogger.debug("Saving cookie to file: {}", cookieFilePath.toAbsolutePath());
		ObjectOutputStream objOutStream = null;
		try {
			objOutStream = new ObjectOutputStream(Files.newOutputStream(cookieFilePath));
			
			myLogger.debug("Number of cookies: {}", cookies.size());
			objOutStream.writeInt(cookies.size());
			
			for (Cookie cookie : cookies) {
				myLogger.debug("Cookie Name {}, Value {}", cookie.getName(), cookie.getValue());
				objOutStream.writeObject(cookie);
			}
			
			objOutStream.flush();
		} catch (IOException e) {
			myLogger.error("Error in saving cookie file", e);
		} finally {
			try {
				objOutStream.close();
			} catch (IOException e) {
				//Do Nothing
			}
		}
	}
	
	public static void loadCookie(Path cookieFilePath, WebDriver webDriver) {
		myLogger.debug("Loading cookie from file: {}", cookieFilePath.toAbsolutePath());
		if (!Files.isReadable(cookieFilePath)) {
			myLogger.debug("Cookie file not exist, so skip loading : {}", cookieFilePath.toAbsolutePath());
			return;
		}
		
		Options webDriverOptions = webDriver.manage();
		ObjectInputStream objInStream = null;
		try {
			objInStream = new ObjectInputStream(Files.newInputStream(cookieFilePath));
			
			int noOfCookies = objInStream.readInt();
			myLogger.debug("Number of cookies in file: {}", noOfCookies);
			
			for (int i=0; i<noOfCookies; i++) {
				Cookie cookie = (Cookie)objInStream.readObject();
				myLogger.debug("Read cookie: Name {}, Value {}", cookie.getName(), cookie.getValue());
				
				webDriverOptions.addCookie(cookie);
			}
		} catch (IOException e) {
			myLogger.error("Error in loading cookie file", e);
		} catch (ClassNotFoundException e) {
			myLogger.error("Error in loading cookie file", e);
		} finally {
			try {
				objInStream.close();
			} catch (IOException e) {
				//Do Nothing
			}
		}
	}
	
	public static void printCookies(WebDriver webDriver) {
		Set<Cookie> cookies = webDriver.manage().getCookies();
		
		myLogger.debug("printAllCookies count: {}", cookies.size());
		
		cookies.forEach((cookie) -> myLogger.debug("Cookie : {}", cookie));
	}
	
	public static void printLocalStorageItems(WebDriver webDriver) {
		JavascriptExecutor js = (JavascriptExecutor)webDriver;
		
		String key = null;
		long count = (Long)js.executeScript("return window.localStorage.length;");
		myLogger.debug("printLocalStorageItems count: {}", count);
		
		for (int i=0; i<count; i++) {
			key = (String)js.executeScript("return window.localStorage.key(" + i + ");");
			myLogger.debug("{}: {}", key, js.executeScript("return window.localStorage.getItem('" + key + "');"));
		}
	}
}
