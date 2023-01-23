package io.github.micwan88.moneytab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import io.github.micwan88.moneytab.messaging.TelegramBot;

public class TelegramBotTest {
	@Test void postNotificationTest() {
		boolean gotRealCredentials = false;
		
		String botToken = System.getProperty(WebBotConst.APP_PROPERTIES_TG_BOT_TOKEN);
		String chatID = System.getProperty(WebBotConst.APP_PROPERTIES_TG_BOT_CHATID);
		if (botToken != null && !botToken.trim().isEmpty() 
				&& chatID != null && !chatID.trim().isEmpty()) {
			gotRealCredentials = true;
		}
		
		assumeTrue(gotRealCredentials);
		
		TelegramBot telegramBot = new TelegramBot(botToken, chatID);
		int retrunCode = telegramBot.postNotification("postNotificationTestTesting msg");
		
		assertEquals(retrunCode, 0, "postNotificationTest");
	}
}
