package io.github.micwan88.moneytab.selenium;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
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
		FirefoxOptions firefoxOptions = new FirefoxOptions();
		
		//Css 'has' selector for firefox need enable 'layout.css.has-selector.enabled'
		firefoxOptions.addPreference("layout.css.has-selector.enabled", true);
		
		//Set it for temp debug
		//firefoxOptions.setLogLevel(FirefoxDriverLogLevel.DEBUG);
		
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
	
	public static boolean loadCookie(Path cookieFilePath, WebDriver webDriver) {
		myLogger.debug("Loading cookie from file: {}", cookieFilePath.toAbsolutePath());
		if (!Files.isReadable(cookieFilePath)) {
			myLogger.debug("Cookie file not exist, so skip loading : {}", cookieFilePath.toAbsolutePath());
			return false;
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
			return noOfCookies > 0;
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
		return false;
	}
	
	public static void printCookies(WebDriver webDriver) {
		Set<Cookie> cookies = webDriver.manage().getCookies();
		
		myLogger.debug("Total cookies count: {}", cookies.size());
		
		cookies.forEach((cookie) -> myLogger.debug("Cookie : {}", cookie));
	}
	
	public static HashMap<String, String> getLocalStorageItems(WebDriver webDriver) {
		JavascriptExecutor jsExecutor = (JavascriptExecutor)webDriver;
		
		long itemCount = (Long)jsExecutor.executeScript("return window.localStorage.length;");
		myLogger.debug("LocalStorageItems count: {}", itemCount);
		
		HashMap<String, String> localStorageItemsMap = new HashMap<>();
		String itemKey = null;
		String itemValue = null;
		for (int i=0; i<itemCount; i++) {
			itemKey = (String)jsExecutor.executeScript("return window.localStorage.key(" + i + ");");
			itemValue = (String)jsExecutor.executeScript("return window.localStorage.getItem('" + itemKey + "');");
			
			localStorageItemsMap.put(itemKey, itemValue);
			myLogger.debug("{}: {}", itemKey, itemValue);
		}
		return localStorageItemsMap;
	}
	
	public static void saveLocalStorageItems(Path localStorageFilePath, HashMap<String, String> localStorageItemsMap) {
		myLogger.debug("Saving localStorage to file: {}", localStorageFilePath.toAbsolutePath());
		ObjectOutputStream objOutStream = null;
		try {
			objOutStream = new ObjectOutputStream(Files.newOutputStream(localStorageFilePath));
			
			myLogger.debug("Number of localStorageItems: {}", localStorageItemsMap.size());
			objOutStream.writeObject(localStorageItemsMap);
			
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
	
	public static boolean loadLocalStorageItems(Path localStorageFilePath, WebDriver webDriver) {
		myLogger.debug("Loading localStorage from file: {}", localStorageFilePath.toAbsolutePath());
		if (!Files.isReadable(localStorageFilePath)) {
			myLogger.debug("LocalStorage file not exist, so skip loading : {}", localStorageFilePath.toAbsolutePath());
			return false;
		}
		
		JavascriptExecutor jsExecutor = (JavascriptExecutor)webDriver;
		ObjectInputStream objInStream = null;
		try {
			objInStream = new ObjectInputStream(Files.newInputStream(localStorageFilePath));
			
			@SuppressWarnings("unchecked")
			HashMap<String, String> localStorageMap = (HashMap<String, String>)objInStream.readObject();
			myLogger.debug("Number of localStorageItem in file: {}", localStorageMap.size());
			
			for (Entry<String, String> mapEntry : localStorageMap.entrySet()) {
				myLogger.debug("Read localStorageItem: Key {}, Value {}", mapEntry.getKey(), mapEntry.getValue());
				
				jsExecutor.executeScript("return window.localStorage.setItem('" + mapEntry.getKey() + "', '" + mapEntry.getValue().replaceAll("\\\\", "\\\\\\\\") + "');");
			}
			
			long count = (Long)jsExecutor.executeScript("return window.localStorage.length;");
			myLogger.debug("Final localStorageItems count: {}", count);
			return count > 0L;
		} catch (IOException e) {
			myLogger.error("Error in loading localStorageItems file", e);
		} catch (ClassNotFoundException e) {
			myLogger.error("Error in loading localStorageItems file", e);
		} catch (Exception e) {
			myLogger.error("Error in loading localStorageItems file", e);
		} finally {
			try {
				objInStream.close();
			} catch (IOException e) {
				//Do Nothing
			}
		}
		return false;
	}
	
	public static void printLocalStorageItems(WebDriver webDriver) {
		getLocalStorageItems(webDriver);
	}
}
