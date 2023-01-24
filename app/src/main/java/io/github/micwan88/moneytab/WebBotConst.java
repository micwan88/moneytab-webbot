package io.github.micwan88.moneytab;

public class WebBotConst {
	public static final String NOTIFICATION_TYPE_NEW_VIDEO = "新到影片";
	public static final String NOTIFICATION_TYPE_IMPORTANT_NEWS = "重要通知";
	
	public static final String NOTIFICATION_DATE_FORMAT_PATTERN = "dd.MM.yyyy";
	public static final String WEBBOT_CHECKSUM_HISTORY_FILENAME = "checksum.dat";
	public static final String WEBBOT_COOKIE_DATA_FILENAME = "cookies.dat";
	public static final String WEBBOT_LOCALSTORAGE_DATA_FILENAME = "lstorage.dat";
	
	//APP PROPERTIES KEY
	public static final String APP_PROPERTIES_BROWSER_TYPE = "moneytab.bot.browserType";
	public static final String APP_PROPERTIES_BROWSER_HEADLESS_MODE = "moneytab.bot.browserHeadlessMode";
	public static final String APP_PROPERTIES_BROWSER_USERDATA = "moneytab.bot.browserUserData"; //Only for chrome currently
	public static final String APP_PROPERTIES_BROWSER_WAIT_TIMEOUT = "moneytab.bot.browserWaitTimeout";
	public static final String APP_PROPERTIES_BROWSER_WAIT_BEFORE_QUIT = "moneytab.bot.browserWaitBeforeQuit";

	public static final String APP_PROPERTIES_SLEEP_TIME = "moneytab.bot.sleepTime";
	public static final String APP_PROPERTIES_LOGIN = "moneytab.bot.login";
	public static final String APP_PROPERTIES_PASSWORD = "moneytab.bot.password";
	
	public static final String APP_PROPERTIES_NOTIFY_DATE_FILTER = "moneytab.bot.notifyDateFilter";
	public static final String APP_PROPERTIES_NOTIFY_TITLE_FILTER = "moneytab.bot.notifyTitleFilter";
	
	public static final String APP_PROPERTIES_TG_BOT_TOKEN = "moneytab.bot.tgBotToken";
	public static final String APP_PROPERTIES_TG_BOT_CHATID = "moneytab.bot.tgBotChatID";
}
