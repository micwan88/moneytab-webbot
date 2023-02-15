package io.github.micwan88.moneytab;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.micwan88.helperclass4j.AppPropertiesUtil;
import io.github.micwan88.moneytab.bean.NotificationItem;
import io.github.micwan88.moneytab.data.NotificationFilter;
import io.github.micwan88.moneytab.messaging.TelegramBot;
import io.github.micwan88.moneytab.selenium.WebDriverMgr;
import io.github.micwan88.moneytab.selenium.WebDriverMgr.DRIVER_TYPE;

public class WebBot implements Closeable {
	
	private static final Logger myLogger = LogManager.getLogger(WebBot.class);
	
	private WebDriver webDriver = null;
	
	private Path checksumHistoryPath = Paths.get(WebBotConst.WEBBOT_CHECKSUM_HISTORY_FILENAME); 
	
	//App Parameters
	private DRIVER_TYPE browserType = DRIVER_TYPE.CHROME;
	private boolean browserHeadlessMode = false;
	private boolean browserPersistCookie = true;
	private boolean browserPersistLocalStorage = true;
	private File browserUserData = null;
	private long waitBeforeQuit = 0L;
	private long waitTimeout = 5000L;
	private long sleepTime = 10000L;
	private String login = "";
	private String password = "";
	private String tgBotToken = null;
	private String tgBotChatID = null;
	
	private NotificationFilter dateFilter = null;
	private NotificationFilter titleFilter = null;
	private NotificationFilter checksumFilter = null;
	
	private long parseLong(String arg) {
		try {
			return Long.parseLong(arg);
		} catch (NumberFormatException e) {
			myLogger.error("Invalid param: {}", arg);
		}
		return 0L;
	}
	
	private void readChecksumHistory(Path checksumHistoryPath) {
		myLogger.debug("Start readChecksumHistory");
		if (Files.isReadable(checksumHistoryPath)) {
			try {
				List<String> checksumList = Files.readAllLines(checksumHistoryPath, Charset.forName("UTF-8"));
				checksumFilter = new NotificationFilter(checksumList, true);
				
				myLogger.debug("Got checksum filter : {}", checksumFilter);
			} catch (IOException e) {
				myLogger.error("Cannot read checksum history file: {}", checksumHistoryPath.toAbsolutePath(), e);
			}
		} else
			myLogger.warn("checksumHistory does not exist or it cannot be read : {}", checksumHistoryPath.toAbsolutePath());
		myLogger.debug("End readChecksumHistory");
	}
	
	public int saveChecksumHistory(Path checksumHistoryPath, List<NotificationItem> notificationItemList) {
		myLogger.debug("Start saveChecksumHistory");
		try (final BufferedWriter bw = Files.newBufferedWriter(checksumHistoryPath, Charset.forName("UTF-8"))) {
			boolean isFirstItem = true;
			int count = 0;
			for (NotificationItem notificationItem : notificationItemList) {
				if (!isFirstItem)
					bw.newLine();
				
				bw.write(notificationItem.getChecksum());
				isFirstItem = false;
				count++;
			}
			
			myLogger.debug("Checksum history saved with count : {}", count);
			return 0;
		} catch (IOException e) {
			myLogger.error("Cannot save checksum history : {}", checksumHistoryPath.toAbsolutePath(), e);
		} finally {
			myLogger.debug("End saveChecksumHistory");
		}
		return -1;
	}
	
	public void persistCookies() {
		myLogger.debug("Start persistCookies");
		if (browserPersistCookie) {
			WebDriverMgr.saveCookie(Paths.get(WebBotConst.WEBBOT_COOKIE_DATA_FILENAME), webDriver.manage().getCookies());
		}
		myLogger.debug("End persistCookies");
	}
	
	public void persistLocalStorageItems() {
		myLogger.debug("Start persistLocalStorageItems");
		if (browserPersistLocalStorage) {
			HashMap<String, String> localStorageItemsMap = WebDriverMgr.getLocalStorageItems(webDriver);
			WebDriverMgr.saveLocalStorageItems(Paths.get(WebBotConst.WEBBOT_LOCALSTORAGE_DATA_FILENAME), localStorageItemsMap);
		}
		myLogger.debug("End persistLocalStorageItems");
	}
	
	public void clearPersistCookiesFile() {
		myLogger.debug("Start clearPersistCookiesFile");
		if (browserPersistCookie) {
			Path targetFilePath = Paths.get(WebBotConst.WEBBOT_COOKIE_DATA_FILENAME);
			try {
				Files.deleteIfExists(targetFilePath);
			} catch (IOException e) {
				myLogger.error("Cannot delete cookie file: {}", targetFilePath.toAbsolutePath(), e);
			}
		}
		myLogger.debug("End clearPersistCookiesFile");
	}
	
	public void clearPersistLocalStorageFile() {
		myLogger.debug("Start clearPersistLocalStorageFile");
		if (browserPersistLocalStorage) {
			Path targetFilePath = Paths.get(WebBotConst.WEBBOT_LOCALSTORAGE_DATA_FILENAME);
			try {
				Files.deleteIfExists(targetFilePath);
			} catch (IOException e) {
				myLogger.error("Cannot delete local storage file: {}", targetFilePath.toAbsolutePath(), e);
			}
		}
		myLogger.debug("End clearPersistLocalStorageFile");
	}
	
	public int loadAppParameters(Properties appProperties) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(WebBotConst.NOTIFICATION_DATE_FORMAT_PATTERN);
		
		String tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_HEADLESS_MODE);
		if (tempStr != null && tempStr.trim().equalsIgnoreCase("true")) {
			browserHeadlessMode = true;
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_TYPE);
		if (tempStr != null && tempStr.trim().equalsIgnoreCase(DRIVER_TYPE.FIREFOX.name())) {
			browserType = DRIVER_TYPE.FIREFOX;
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_USERDATA);
		if (tempStr != null && !tempStr.trim().equals("")) {
			browserUserData = new File(tempStr);
			
			if (!browserUserData.isDirectory() && !browserUserData.mkdirs()) {
				myLogger.error("Cannot create browser user data directory {}: {}", WebBotConst.APP_PROPERTIES_BROWSER_USERDATA, browserUserData.getAbsolutePath());
				return -1;
			}
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_WAIT_TIMEOUT);
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			waitTimeout = parseLong(tempStr);
			
			if (waitTimeout == 0L) {
				myLogger.error("Invalid {}: {}", WebBotConst.APP_PROPERTIES_BROWSER_WAIT_TIMEOUT, tempStr);
				return -1;
			}
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_WAIT_BEFORE_QUIT);
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			waitBeforeQuit = parseLong(tempStr); //Can be 'zero'
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_PERSIST_COOKIE);
		if (tempStr != null && tempStr.trim().equalsIgnoreCase("false")) {
			browserPersistCookie = false;
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_PERSIST_LOCAL_STORAGE);
		if (tempStr != null && tempStr.trim().equalsIgnoreCase("false")) {
			browserPersistLocalStorage = false;
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_SLEEP_TIME);
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			sleepTime = parseLong(tempStr);
			
			if (sleepTime == 0L) {
				myLogger.error("Invalid {}: {}", WebBotConst.APP_PROPERTIES_SLEEP_TIME, tempStr);
				return -1;
			}
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_LOGIN);
		if (tempStr != null && !tempStr.trim().equals("")) {
			login = tempStr.trim();
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_PASSWORD);
		if (tempStr != null && !tempStr.trim().equals("")) {
			password = tempStr.trim();
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_NOTIFY_DATE_FILTER);
		if (tempStr != null && !tempStr.trim().equals("")) {
			dateFilter = new NotificationFilter(tempStr.trim().replaceAll("TODAY", dateFormat.format(new Date())));
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_NOTIFY_TITLE_FILTER);
		if (tempStr != null && !tempStr.trim().equals("")) {
			titleFilter = new NotificationFilter(tempStr.trim());
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_TG_BOT_TOKEN);
		if (tempStr != null && !tempStr.trim().equals("")) {
			tgBotToken = tempStr.trim();
		}
		
		tempStr = appProperties.getProperty(WebBotConst.APP_PROPERTIES_TG_BOT_CHATID);
		if (tempStr != null && !tempStr.trim().equals("")) {
			tgBotChatID = tempStr.trim();
		}
		
		/**
		 * System properties "-D" value will override the app.properties
		 */
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_HEADLESS_MODE);
		if (tempStr != null) {
			if (tempStr.trim().equalsIgnoreCase("true")) {
				browserHeadlessMode = true;
			} else {
				browserHeadlessMode = false;
			}
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_TYPE);
		if (tempStr != null) {
			if (tempStr.trim().equalsIgnoreCase(DRIVER_TYPE.FIREFOX.name())) {
				browserType = DRIVER_TYPE.FIREFOX;
			} else {
				browserType = DRIVER_TYPE.CHROME;
			}
		}	
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_USERDATA);
		if (tempStr != null && !tempStr.trim().equals("")) {
			browserUserData = new File(tempStr);
			
			if (!browserUserData.isDirectory() && !browserUserData.mkdirs()) {
				myLogger.error("Cannot create browser user data directory {}: {}", WebBotConst.APP_PROPERTIES_BROWSER_USERDATA, browserUserData.getAbsolutePath());
				return -1;
			}
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_WAIT_TIMEOUT);
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			waitTimeout = parseLong(tempStr);
			
			if (waitTimeout == 0L) {
				myLogger.error("Invalid {}: {}", WebBotConst.APP_PROPERTIES_BROWSER_WAIT_TIMEOUT, tempStr);
				return -1;
			}
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_WAIT_BEFORE_QUIT);
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			waitBeforeQuit = parseLong(tempStr); //Can be 'zero'
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_PERSIST_COOKIE);
		if (tempStr != null) {
			if (tempStr.trim().equalsIgnoreCase("false")) {
				browserPersistCookie = false;
			} else {
				browserPersistCookie = true;
			}
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_BROWSER_PERSIST_LOCAL_STORAGE);
		if (tempStr != null) {
			if (tempStr.trim().equalsIgnoreCase("false")) {
				browserPersistLocalStorage = false;
			} else {
				browserPersistLocalStorage = true;
			}
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_SLEEP_TIME);
		if (tempStr != null && !tempStr.trim().isEmpty()) {
			sleepTime = parseLong(tempStr);
			
			if (sleepTime == 0L) {
				myLogger.error("Invalid {}: {}", WebBotConst.APP_PROPERTIES_SLEEP_TIME, tempStr);
				return -1;
			}
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_LOGIN);
		if (tempStr != null && !tempStr.trim().equals("")) {
			login = tempStr.trim();
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_PASSWORD);
		if (tempStr != null && !tempStr.trim().equals("")) {
			password = tempStr.trim();
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_NOTIFY_DATE_FILTER);
		if (tempStr != null && !tempStr.trim().equals("")) {
			dateFilter = new NotificationFilter(tempStr.trim().replaceAll("TODAY", dateFormat.format(new Date())));
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_NOTIFY_TITLE_FILTER);
		if (tempStr != null && !tempStr.trim().equals("")) {
			titleFilter = new NotificationFilter(tempStr.trim());
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_TG_BOT_TOKEN);
		if (tempStr != null && !tempStr.trim().equals("")) {
			tgBotToken = tempStr.trim();
		}
		
		tempStr = System.getProperty(WebBotConst.APP_PROPERTIES_TG_BOT_CHATID);
		if (tempStr != null && !tempStr.trim().equals("")) {
			tgBotChatID = tempStr.trim();
		}
		
		return 0;
	}
	
	public void debugParams() {
		myLogger.debug("Debug Params ...");
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_BROWSER_TYPE, browserType);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_BROWSER_HEADLESS_MODE, browserHeadlessMode);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_BROWSER_USERDATA, browserUserData != null ? browserUserData.getAbsolutePath() : null);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_BROWSER_WAIT_TIMEOUT, waitTimeout);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_BROWSER_WAIT_BEFORE_QUIT, waitBeforeQuit);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_BROWSER_PERSIST_COOKIE, browserPersistCookie);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_BROWSER_PERSIST_LOCAL_STORAGE, browserPersistLocalStorage);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_SLEEP_TIME, sleepTime);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_LOGIN, login);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_PASSWORD, password);
		
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_NOTIFY_DATE_FILTER, dateFilter);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_NOTIFY_TITLE_FILTER, titleFilter);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_TG_BOT_TOKEN, tgBotToken);
		myLogger.debug("AppProp - {}: {}" , WebBotConst.APP_PROPERTIES_TG_BOT_CHATID, tgBotChatID);
	}
	
	public static void main(String[] args) {
		myLogger.debug("Loading appProperties ...");
		AppPropertiesUtil appPropertyUtil = new AppPropertiesUtil();
		Properties appProperties = appPropertyUtil.getAppProperty();
		if (appProperties == null) {
			myLogger.error("Cannot load appProperties: {}", AppPropertiesUtil.APP_PROPERTY_FILE);
			System.exit(-1);
		}
		
		boolean needClearBrowserState = false;
		WebBot webBot = new WebBot();
		
		int returnCode = webBot.loadAppParameters(appProperties);
		
		webBot.debugParams();
		
		if (returnCode != 0) {
			myLogger.error("Invalid parameters");
			System.exit(-2);
		}
		
		try {
			webBot.init();
			
			//Load previous state if necessary
			webBot.loadPreviousBrowserState();
			
			boolean result = webBot.loginMoneyTabWeb(webBot.getLogin(), webBot.getPassword());
			
			//If logon success
			if (result) {
				List<NotificationItem> notificationItemList = webBot.extractNotificationList(webBot.getDateFilter(), webBot.getTitleFilter());
				
				if (notificationItemList != null) {
					//Filter by checksum
					List<NotificationItem> outNotificationItems = notificationItemList.stream().filter((notificationItem) -> {
						if (webBot.getChecksumFilter() == null)
							return true;
						else
							return webBot.getChecksumFilter().filterChecksum(notificationItem);
					}).collect(Collectors.toList());
					
					//Mark items not yet sent
					outNotificationItems.forEach((notificationItem) -> notificationItem.setSent(false));
					
					myLogger.debug("Filtered by checksum outNotificationItems.size : {}", outNotificationItems.size());
					
					//Extract YouTube link
					returnCode = webBot.populateYoutubeLink(outNotificationItems);
					
					if (returnCode == 0) {
						//Send TG msg
						returnCode = webBot.sendTelegramNotification(outNotificationItems, webBot.getTgBotToken(), webBot.getTgBotChatID());
						
						//Save the checksum for next run to prevent duplicate sending
						if (returnCode == 0)
							returnCode = webBot.saveChecksumHistory(webBot.getChecksumHistoryPath(), notificationItemList);
						
						//Try load home again before save stated to file
						webBot.loadMoneyTabWebHome();
						
						//Persist browser state
						webBot.persistCookies();
						webBot.persistLocalStorageItems();
					}
				} else {
					returnCode = -999;
				}
			}
			
			if (!result || returnCode != 0)
				needClearBrowserState = true;
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
			needClearBrowserState = true;
		} finally {
			webBot.close();
			myLogger.debug("WebBot End");
		}
		
		//When something weird happen, just clear cookie/local storage file
		if (needClearBrowserState) {
			webBot.clearPersistCookiesFile();
			webBot.clearPersistLocalStorageFile();
		}
		
		if (returnCode != 0)
			System.exit(-3);
	}
	
	public void init() {
		WebDriverMgr webDriverMgr = new WebDriverMgr();
		ArrayList<String> driverOptions = new ArrayList<>();
		
		if (browserType.equals(DRIVER_TYPE.FIREFOX)) {
			webDriver = webDriverMgr.getWebDriver(DRIVER_TYPE.FIREFOX, browserHeadlessMode, driverOptions);
		} else {
			if (browserUserData != null)
				driverOptions.add("user-data-dir=" + browserUserData.getAbsolutePath());
			
			driverOptions.add("--disable-gpu");
			driverOptions.add("--disable-extensions");
			//driverOptions.add("--disable-file-system");
			//driverOptions.add("--disable-media-session-api");
			//driverOptions.add("--disable-shared-workers");
			//driverOptions.add("--disable-threaded-compositing");
			//driverOptions.add("--disable-threaded-scrolling");
			//driverOptions.add("--disable-v8-idle-tasks");
			//driverOptions.add("--disable-volume-adjust-sound");
			//driverOptions.add("--disable-webgl");
			driverOptions.add("--no-default-browser-check");
			//driverOptions.add("--no-experiments");
			
			webDriver = webDriverMgr.getWebDriver(DRIVER_TYPE.CHROME, browserHeadlessMode, driverOptions);
		}
		
		//Read the checksum history and convert it to filter
		readChecksumHistory(checksumHistoryPath);
	}

	@Override
	public void close() {
		if (webDriver != null) {
			if (waitBeforeQuit != 0L) {
				try {
					myLogger.debug("Sleep before quit driver : {}", waitBeforeQuit);
					Thread.sleep(waitBeforeQuit);
				} catch (InterruptedException e) {
					//Do nothing
				}
			}
			
			webDriver.quit();
		}
	}
	
	public boolean loadMoneyTabWebHome() {
		myLogger.debug("Start loadMoneyTabWebHome");
		
		String targetURL = "https://www.money-tab.com";
		myLogger.debug("Target URL: {}", targetURL);
		
		try {
			webDriver.get(targetURL);
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
		} finally {
			myLogger.debug("End loadMoneyTabWebHome");
		}
		return false;
	}
	
	public void loadPreviousBrowserState() {
		myLogger.debug("Start loadPreviousBrowserState");
		
		if (browserPersistCookie | browserPersistLocalStorage) {
			loadMoneyTabWebHome();
			
			if (browserPersistCookie) {
				WebDriverMgr.loadCookie(Paths.get(WebBotConst.WEBBOT_COOKIE_DATA_FILENAME), webDriver);
			}
			
			if (browserPersistLocalStorage) {
				WebDriverMgr.loadLocalStorageItems(Paths.get(WebBotConst.WEBBOT_LOCALSTORAGE_DATA_FILENAME), webDriver);
			}
		}
		
		myLogger.debug("End loadPreviousBrowserState");
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
					.until(driver -> driver.findElement(By.cssSelector("section > div > div > span[role='button']:has(span)")));
			
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
	
	//We cannot put checksum filter here, bcoz we still need get to same list of checksum to save in file through out a day
	public List<NotificationItem> extractNotificationList(final NotificationFilter notifyDateFilter, final NotificationFilter notifyTitleFilter) {
		myLogger.debug("Start extractNotificationList");
		
		String targetURL = "https://www.money-tab.com/profile/notification";
		myLogger.debug("Target URL: {}", targetURL);
		
		try {
			webDriver.get(targetURL);
			
			myLogger.debug("Check if the page would be redriect due to unauthorize or something ...");
			
			if (!checkIfPageURLMatched(targetURL)) {
				myLogger.error("Notification page has been redirect to : {}", webDriver.getCurrentUrl());
				return null;
			}
			
			myLogger.debug("Finding notification div container");
			
			//Need time to load, so need wait
			WebElement notificationDiv = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(driver -> driver.findElement(By.cssSelector("section > div > div > p + div")));
			
			myLogger.debug("notificationDiv found, try get list of notification items ...");
			
			List<WebElement> notificationItemElementList = notificationDiv.findElements(By.cssSelector("div[class^='notice_item']"));
			
			myLogger.debug("notificationItemElementList.size : {}", notificationItemElementList.size());
			
			ArrayList<NotificationItem> notificationItemList = new ArrayList<>();
			notificationItemElementList.forEach((notificationItemElement) -> {
				WebElement notificationLinkElement = null;
				WebElement notificationDateElement = null;
				WebElement notificationTitleElement = null;
				WebElement notificationNoLinkDivElement = null;
				NotificationItem notificationItem = new NotificationItem();
				WebElement notificationTypeElement = notificationItemElement.findElement(By.cssSelector("div > span + span"));
				if (notificationTypeElement.getText().trim().equals(WebBotConst.NOTIFICATION_TYPE_NEW_VIDEO)) {
					notificationLinkElement = notificationItemElement.findElement(By.cssSelector("div + div > a.block[href]"));
					notificationDateElement = notificationLinkElement.findElement(By.cssSelector("div > span"));
					notificationTitleElement = notificationLinkElement.findElement(By.cssSelector("div:has(span) + p"));
					
					notificationItem.setType(notificationTypeElement.getText().trim());
					notificationItem.setDateInString(notificationDateElement.getText().trim());
					notificationItem.setTitle(notificationTitleElement.getText().trim());
					notificationItem.setFullDescription(notificationLinkElement.getText().trim());
					notificationItem.setPageLink(notificationLinkElement.getAttribute("href").trim());
					
					notificationItem.setChecksum(DigestUtils.sha256Hex(notificationItem.getFullDescription()));
				} else {
					//No link if just news notification
					notificationNoLinkDivElement = notificationItemElement.findElement(By.cssSelector("div + div"));
					notificationDateElement = notificationNoLinkDivElement.findElement(By.cssSelector("div > span"));
					notificationTitleElement = notificationNoLinkDivElement.findElement(By.cssSelector("div:has(span) + p"));
					
					notificationItem.setType(notificationTypeElement.getText().trim());
					notificationItem.setDateInString(notificationDateElement.getText().trim());
					notificationItem.setTitle(notificationTitleElement.getText().trim());
					notificationItem.setFullDescription(notificationNoLinkDivElement.getText().trim());
					
					notificationItem.setChecksum(DigestUtils.sha256Hex(notificationItem.getFullDescription()));
				}
				
				if (notifyDateFilter != null && !notifyDateFilter.filterDate(notificationItem)) {
					myLogger.debug("Filtered by date: {}", notificationItem);
					return;
				}
				
				//Title filter only apply for new video
				if (notificationItem.getType().equals(WebBotConst.NOTIFICATION_TYPE_NEW_VIDEO)) {
					if (notifyTitleFilter != null && !notifyTitleFilter.filterTitle(notificationItem)) {
						myLogger.debug("Filtered by title: {}", notificationItem);
						return;
					}
				}
				
				notificationItemList.add(notificationItem);
				myLogger.debug("Add to list : {}", notificationItem);
			});
			
			myLogger.debug("Output notificationItemList.size : {}", notificationItemList.size());
			return notificationItemList;
		} catch (NoSuchElementException e) {
			myLogger.error("Cannot find related element in : " + webDriver.getTitle(), e);
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
		} finally {
			myLogger.debug("End extractNotificationList");
		}
		return null;
	}
	
	public int populateYoutubeLink(List<NotificationItem> notificationItemList) {
		myLogger.debug("Start populateYoutubeLink");
		
		try {
			for (NotificationItem notificationItem : notificationItemList) {
				//Skip if non video item
				if (notificationItem.getPageLink() == null)
					continue;
				
				String targetURL = notificationItem.getPageLink();
				myLogger.debug("Target URL: {}", targetURL);
				
				webDriver.get(targetURL);
				
				//Try check if logon profile to ensure page loaded fully
				if (!checkIfAlreadyLogon())
					return -1;
				
				//Need time to load, so need wait
				WebElement iFrameElement = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
						.until(driver -> driver.findElement(By.cssSelector("main > section > div + div iframe")));
				
				myLogger.debug("iFrameElement found : {} - src {}", iFrameElement.getTagName(), iFrameElement.getAttribute("src"));
				
				myLogger.debug("Switch to iFrameElement ...");
				webDriver.switchTo().frame(iFrameElement);
				
				String youTubeLink = tryExtractYoutubeLink();
				
				if (youTubeLink == null) {
					//Find any nested iframe
					iFrameElement = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
							.until(driver -> driver.findElement(By.cssSelector("iframe")));
					
					myLogger.debug("iFrameElement found : {} - src {}", iFrameElement.getTagName(), iFrameElement.getAttribute("src"));
					
					myLogger.debug("Switch to iFrameElement ...");
					webDriver.switchTo().frame(iFrameElement);
					
					youTubeLink = tryExtractYoutubeLink();
					if (youTubeLink == null) {
						myLogger.error("Still cannot find the youtube link item from : {}", notificationItem);
						return -2;
					}
				}
				
				notificationItem.setVideoLink(youTubeLink);
			}
			
			return 0;
		} catch (NoSuchElementException e) {
			myLogger.error("Cannot find related element in : " + webDriver.getTitle(), e);
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
		} finally {
			myLogger.debug("End populateYoutubeLink");
		}
		return -3;
	}
	
	public int sendTelegramNotification(List<NotificationItem> notificationItemList, String tgBotToken, String tgBotChatID) {
		TelegramBot telegramBot = new TelegramBot(tgBotToken);
		
		int returnCode = 0;
		for (NotificationItem notificationItem : notificationItemList) {
			returnCode = telegramBot.postNotifications(constructOutMsg(notificationItem), tgBotChatID);
			
			if (returnCode != 0)
				return returnCode;
		}
		
		return 0;
	}
	
	private String constructOutMsg(NotificationItem notificationItem) {
		StringBuffer outMsg = new StringBuffer();
		
		outMsg.append(notificationItem.getFullDescription());
		
		if (notificationItem.getVideoLink() != null) {
			outMsg.append("\n\n");
			outMsg.append(notificationItem.getVideoLink());
		}
		
		return outMsg.toString();
	}
	
	private String tryExtractYoutubeLink() {
		myLogger.debug("Start tryExtractYoutubeLink");
		try {
			myLogger.debug("Try to extract any youtube link ...");
			
			//Need time to load, so need wait
			WebElement linkElement = new WebDriverWait(webDriver, Duration.ofMillis(waitTimeout))
					.until(driver -> driver.findElement(By.cssSelector("link[rel='canonical']")));
			
			String youtubeLink = linkElement.getAttribute("href");
			myLogger.debug("Youtube link found : {}", youtubeLink);
			return youtubeLink;
		} catch (TimeoutException tie) {
			//Nothing
		}
		myLogger.debug("Youtube link not found");
		myLogger.debug("End tryExtractYoutubeLink");
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

	public Path getChecksumHistoryPath() {
		return checksumHistoryPath;
	}

	public void setChecksumHistoryPath(Path checksumHistoryPath) {
		this.checksumHistoryPath = checksumHistoryPath;
	}

	public NotificationFilter getChecksumFilter() {
		return checksumFilter;
	}

	public void setChecksumFilter(NotificationFilter checksumFilter) {
		this.checksumFilter = checksumFilter;
	}

	public boolean isBrowserHeadlessMode() {
		return browserHeadlessMode;
	}

	public DRIVER_TYPE getBrowserType() {
		return browserType;
	}

	public void setBrowserType(DRIVER_TYPE browserType) {
		this.browserType = browserType;
	}

	public void setBrowserHeadlessMode(boolean browserHeadlessMode) {
		this.browserHeadlessMode = browserHeadlessMode;
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

	public long getWaitBeforeQuit() {
		return waitBeforeQuit;
	}

	public void setWaitBeforeQuit(long waitBeforeQuit) {
		this.waitBeforeQuit = waitBeforeQuit;
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

	public NotificationFilter getDateFilter() {
		return dateFilter;
	}

	public void setDateFilter(NotificationFilter dateFilter) {
		this.dateFilter = dateFilter;
	}

	public NotificationFilter getTitleFilter() {
		return titleFilter;
	}

	public void setTitleFilter(NotificationFilter titleFilter) {
		this.titleFilter = titleFilter;
	}

	public String getTgBotToken() {
		return tgBotToken;
	}

	public void setTgBotToken(String tgBotToken) {
		this.tgBotToken = tgBotToken;
	}

	public String getTgBotChatID() {
		return tgBotChatID;
	}

	public void setTgBotChatID(String tgBotChatID) {
		this.tgBotChatID = tgBotChatID;
	}
}
