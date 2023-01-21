package io.github.micwan88.moneytab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.github.micwan88.moneytab.bean.NotificationItem;

@TestInstance(Lifecycle.PER_CLASS)
class WebBotTest {
	WebBot webBot = new WebBot();
	boolean gotRealCredentials = false;
	
	@BeforeAll void beforeTest() throws IOException {
		//Hard code test properties
		Properties appProperties = new Properties();
		appProperties.put(WebBotConst.APP_PROPERTIES_BROWSER_HEADLESS_MODE, "false");
		appProperties.put(WebBotConst.APP_PROPERTIES_BROWSER_DETACH_MODE, "false");
		appProperties.put(WebBotConst.APP_PROPERTIES_BROWSER_WAIT_TIMEOUT, "8000");
		appProperties.put(WebBotConst.APP_PROPERTIES_SLEEP_TIME, "10000");
		//Fake username and password
		appProperties.put(WebBotConst.APP_PROPERTIES_LOGIN, "aaaa");
		appProperties.put(WebBotConst.APP_PROPERTIES_PASSWORD, "bbbb");
		
		appProperties.put(WebBotConst.APP_PROPERTIES_NOTIFY_DATE_FILTER, "TODAY");
		appProperties.put(WebBotConst.APP_PROPERTIES_NOTIFY_TITLE_FILTER, "^90後零至千萬的故事,我要做磚家,我要炒股票,贏在美股系列,我要做屋主");
		
		/**
		 * Don't set userdata directory as don't want browser retain the last logon stage for test
		 * But if intend to retain browser state, then can specify in system properties to override the setting
		 */
		//appProperties.put(WebBotConst.APP_PROPERTIES_BROWSER_USERDATA, "userdata");
		
		webBot.loadAppParameters(appProperties);
		
		webBot.debugParams();
		
		webBot.init();
		
		String realUsername = System.getProperty(WebBotConst.APP_PROPERTIES_LOGIN);
		String realPassword = System.getProperty(WebBotConst.APP_PROPERTIES_PASSWORD);
		if (realUsername != null && !realUsername.trim().isEmpty() 
				&& realPassword != null && !realPassword.trim().isEmpty()) {
			gotRealCredentials = true;
		}
	}
	
	@AfterAll void afterTest() {
		webBot.close();
	}
	
	@Test void loginNotEmptyTest() {
		assertNotEquals(webBot.getLogin(), "", "loginNotEmptyTest");
	}
	
	@Test void passwordNotEmptyTest() {
		assertNotEquals(webBot.getPassword(), "", "passwordNotEmptyTest");
	}
	
    @Test void logonFailTest() {
    	assumeFalse(gotRealCredentials);
    	
    	String username = webBot.getLogin();
    	String password = webBot.getPassword();
    	
    	boolean logonResult = webBot.loginMoneyTabWeb(username, password);
        assertFalse(logonResult, "loginMoneyTabWeb");
    }
    
    @Test void logonSuccessTest() {
    	assumeTrue(gotRealCredentials);
    	
    	String username = webBot.getLogin();
    	String password = webBot.getPassword();
    	
    	boolean logonResult = webBot.loginMoneyTabWeb(username, password);
        assertTrue(logonResult, "loginMoneyTabWeb");
    }
    
    @Test void extractNotificationTest() {
    	assumeTrue(gotRealCredentials);
    	
    	//Need logon success 
    	String username = webBot.getLogin();
    	String password = webBot.getPassword();
    	boolean logonResult = webBot.loginMoneyTabWeb(username, password);
        assertTrue(logonResult, "extractNotificationTest - logon");
        
        List<NotificationItem> notificationItemList = webBot.extractNotificationList();
        assertNotNull(notificationItemList, "extractNotificationTest - notificationItemList");
    }
}
