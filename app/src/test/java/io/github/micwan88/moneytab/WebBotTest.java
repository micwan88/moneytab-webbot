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
		appProperties.put("moneytab.bot.browserHeadlessMode", "false");
		appProperties.put("moneytab.bot.browserDetachMode", "false");
		appProperties.put("moneytab.bot.browserWaitTimeout", "8000");
		appProperties.put("moneytab.bot.sleepTime", "10000");
		//Fake username and password
		appProperties.put("moneytab.bot.login", "aaaa");
		appProperties.put("moneytab.bot.password", "bbbb");
		
		/**
		 * Don't set userdata directory as don't want browser retain the last logon stage for test
		 * But if intend to retain browser state, then can specify in system properties to override the setting
		 */
		//appProperties.put("moneytab.bot.browserUserData", "userdata");
		
		webBot.loadAppParameters(appProperties);
		
		webBot.debugParams();
		
		webBot.init();
		
		String realUsername = System.getProperty("moneytab.bot.login");
		String realPassword = System.getProperty("moneytab.bot.password");
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
