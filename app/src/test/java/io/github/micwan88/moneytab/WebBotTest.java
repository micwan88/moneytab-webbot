/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.github.micwan88.moneytab;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		
		webBot.loadAppParameters(appProperties);
		
		webBot.debugParams();
		
		webBot.init();
	}
	
	@AfterAll void afterTest() {
		webBot.close();
	}
	
    @Test void webBotTest() {
        assertNotNull(webBot.loadMoneyTabWeb(), "loadMoneyTabWeb");
    }
}
