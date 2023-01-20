package io.github.micwan88.moneytab;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.micwan88.helperclass4j.AppPropertiesUtil;

public class WebBot implements Closeable {
	
	private static final Logger myLogger = LogManager.getLogger(WebBot.class);
	
	private ChromeDriverService chromeDriverService = null;
	private WebDriver webDriver = null;
	
	//App Parameters
	private boolean browserHeadlessMode = false;
	private boolean browserDetachMode = false;
	private File browserUserData = null;
	private long waitTimeout = 5000L;
	private long sleepTime = 10000L;
	private String login = "";
	private String password = "";
	
	private long parseLong(String arg) {
		try {
			return Long.parseLong(arg);
		} catch (NumberFormatException e) {
			myLogger.error("Invalid param: {}", arg);
		}
		return 0L;
	}
	
	public int loadAppParameters(Properties appProperties) {
		String tempStr = appProperties.getProperty("moneytab.bot.browserHeadlessMode");
		if (tempStr != null && tempStr.trim().equalsIgnoreCase("true")) {
			browserHeadlessMode = true;
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.browserDetachMode");
		if (tempStr != null && tempStr.trim().equalsIgnoreCase("true")) {
			browserDetachMode = true;
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.browserUserData");
		if (tempStr != null && !tempStr.trim().equals("")) {
			browserUserData = new File(tempStr);
			
			if (!browserUserData.isDirectory() || !browserUserData.exists()) {
				myLogger.error("Browser user data not exist {}: {}", "moneytab.bot.browserUserData", browserUserData.getAbsolutePath());
				return -1;
			}
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.browserWaitTimeout");
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			waitTimeout = parseLong(tempStr);
			
			if (waitTimeout == 0L) {
				myLogger.error("Invalid moneytab.bot.browserWaitTimeout: {}", tempStr);
				return -1;
			}
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.sleepTime");
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			sleepTime = parseLong(tempStr);
			
			if (sleepTime == 0L) {
				myLogger.error("Invalid moneytab.bot.sleepTime: {}", tempStr);
				return -1;
			}
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.login");
		if (tempStr != null && !tempStr.trim().equals("")) {
			login = tempStr.trim();
		}
		
		tempStr = appProperties.getProperty("moneytab.bot.password");
		if (tempStr != null && !tempStr.trim().equals("")) {
			password = tempStr.trim();
		}
		
		//System properties "-D" value will override the app.properties
		tempStr = System.getProperty("moneytab.bot.login");
		if (tempStr != null && !tempStr.trim().equals("")) {
			login = tempStr.trim();
		}
		
		tempStr = System.getProperty("moneytab.bot.password");
		if (tempStr != null && !tempStr.trim().equals("")) {
			password = tempStr.trim();
		}
		
		return 0;
	}
	
	public void debugParams() {
		myLogger.debug("Debug Params ...");
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.browserHeadlessMode", browserHeadlessMode);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.browserDetachMode", browserDetachMode);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.browserUserData", browserUserData != null ? browserUserData.getAbsolutePath() : null);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.browserWaitTimeout", waitTimeout);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.sleepTime", sleepTime);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.login", login);
		myLogger.debug("AppProp - {}: {}" , "moneytab.bot.password", password);
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
			
			boolean result = webBot.loginMoneyTabWeb(webBot.getLogin(), webBot.getPassword());
		} catch (IOException e) {
			myLogger.error("Chromedriver init error", e);
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
		} finally {
			webBot.close();
		}
		
		if (returnCode != 0)
			System.exit(-3);
	}
	
	public void init() throws IOException {
		chromeDriverService = new ChromeDriverService.Builder().usingAnyFreePort().build();
		chromeDriverService.start();
		
		ChromeOptions chromeOptions = new ChromeOptions();
		//Not sure why we need this when in Linux, may be bug of chromedriver ?
		chromeOptions.addArguments("--remote-debugging-port=9222");
		
		if (browserHeadlessMode)
			chromeOptions.setHeadless(true);
		
		if (browserUserData != null)
			chromeOptions.addArguments("user-data-dir=" + browserUserData.getAbsolutePath());
		
		if (browserDetachMode)
			chromeOptions.setExperimentalOption("detach", true);
		
		webDriver = new ChromeDriver(chromeDriverService, chromeOptions);
	}

	@Override
	public void close() {
		//Not close brower when run in detach mode
		if (!browserDetachMode && webDriver != null)
			webDriver.quit();
		
		if (chromeDriverService != null)
			chromeDriverService.stop();
	}
	
	public boolean loginMoneyTabWeb(String username, String password) {
		myLogger.debug("Start loginMoneyTabWeb");
		
		String targetURL = "https://www.money-tab.com";
		myLogger.debug("Target URL: {}", targetURL);
		
		try {
			webDriver.get(targetURL);
			
			if (checkIfAlreadyLogon())
				return true;
			
			myLogger.debug("Checking if loginButton here");
			
			WebElement loginButton = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(driver -> driver.findElement(By.cssSelector("section > div > div > span[role='button']:has(> span)")));
			
			String buttonTitle = loginButton.getText();
			if (!buttonTitle.equals("登入")) {
				myLogger.error("Cannot find login button : {}", webDriver.getTitle());
				return false;
			}
			
			myLogger.debug("loginButton found.");
			
			loginButton.click();
			
			WebElement loginForm = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(driver -> driver.findElement(By.cssSelector("form:has(input#username)")));
			
			myLogger.debug("loginForm found.");
			
			WebElement usernameField = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(ExpectedConditions.visibilityOf(loginForm.findElement(By.cssSelector("input#username"))));
			
			WebElement passwdField = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(ExpectedConditions.visibilityOf(loginForm.findElement(By.cssSelector("input#password"))));
			
			myLogger.debug("usernameField, passwdField found.");
			
			usernameField.sendKeys(username);
			passwdField.sendKeys(password);
			
			WebElement submitButton = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(ExpectedConditions.elementToBeClickable(loginForm.findElement(By.cssSelector("button[type='submit']"))));
			
			myLogger.debug("submitButton found and try login ...");
			
			submitButton.click();
			
			if (checkIfAlreadyLogon())
				return true;
			
			//Login failed
			WebElement loginResult = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(ExpectedConditions.visibilityOf(loginForm.findElement(By.cssSelector("div > div.input-row + div.input-row + div"))));
			myLogger.error("Login failed result: {}", loginResult.getText());
		} catch (NoSuchElementException e) {
			myLogger.error("Cannot find related element in : " + webDriver.getTitle(), e);
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
		} finally {
			myLogger.debug("End loginMoneyTabWeb");
		}
		return false;
	}
	
	public List<String> extractNotificationList() {
		myLogger.debug("Start extractNotificationList");
		
		String targetURL = "https://www.money-tab.com/profile/notification";
		myLogger.debug("Target URL: {}", targetURL);
		
		try {
			webDriver.get(targetURL);
			
			if (checkIfPageURLMatched(targetURL)) {
				myLogger.error("Notification page has been redirect to : {}", webDriver.getCurrentUrl());
				return null;
			}
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
		} finally {
			myLogger.debug("End extractNotificationList");
		}
		return null;
	}
	
	private boolean checkIfAlreadyLogon() {
		myLogger.debug("Start checkIfAlreadyLogon");
		try {
			myLogger.debug("Try find profile account link ...");
			
			WebElement profileLink = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href='/profile/account']:has(svg.svg-icon)")));
			
			myLogger.debug("Logon profileLink is displayed : {}", profileLink.isDisplayed());
			return true;
		} catch (TimeoutException tie) {
			//Nothing
		}
		myLogger.debug("Profile account link not here");
		myLogger.debug("End loginMoneyTabWeb");
		return false;
	}
	
	private boolean checkIfPageURLMatched(String targetURL) {
		myLogger.debug("Start checkIfPageURLMatched");
		try {
			return new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(ExpectedConditions.urlToBe(targetURL));
		} catch (TimeoutException tie) {
			//Nothing
		} finally {
			myLogger.debug("End checkIfPageURLMatched");
		}
		return false;
	}

	public boolean isBrowserHeadlessMode() {
		return browserHeadlessMode;
	}

	public void setBrowserHeadlessMode(boolean browserHeadlessMode) {
		this.browserHeadlessMode = browserHeadlessMode;
	}

	public boolean isBrowserDetachMode() {
		return browserDetachMode;
	}

	public void setBrowserDetachMode(boolean browserDetachMode) {
		this.browserDetachMode = browserDetachMode;
	}

	public File getBrowserUserData() {
		return browserUserData;
	}

	public void setBrowserUserData(File browserUserData) {
		this.browserUserData = browserUserData;
	}

	public long getWaitTimeout() {
		return waitTimeout;
	}

	public void setWaitTimeout(long waitTimeout) {
		this.waitTimeout = waitTimeout;
	}

	public long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}
}
