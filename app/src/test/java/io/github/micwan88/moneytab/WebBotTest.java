package io.github.micwan88.moneytab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class WebBotTest {
	WebBot webBot = new WebBot();
	
	@BeforeAll void beforeTest() throws IOException {
		//Hard code test properties
		Properties appProperties = new Properties();
		appProperties.put("moneytab.bot.browserHeadlessMode", "false");
		appProperties.put("moneytab.bot.browserUserData", "userdata");
		appProperties.put("moneytab.bot.browserDetachMode", "false");
		appProperties.put("moneytab.bot.browserWaitTimeout", "8000");
		appProperties.put("moneytab.bot.sleepTime", "10000");
		appProperties.put("moneytab.bot.login", "aaaa");
		appProperties.put("moneytab.bot.password", "bbbb");
		
		webBot.loadAppParameters(appProperties);
		
		webBot.debugParams();
		
		webBot.init();
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
	
    @Test void webBotTest() {
    	String username = webBot.getLogin();
    	String password = webBot.getPassword();
    	
    	boolean logonResult = webBot.loginMoneyTabWeb(username, password);
        assertFalse(logonResult, "loginMoneyTabWeb");
    }
}
