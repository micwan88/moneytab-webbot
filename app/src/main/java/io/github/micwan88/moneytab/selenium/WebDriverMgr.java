package io.github.micwan88.moneytab.selenium;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

public class WebDriverMgr {
	
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
}
