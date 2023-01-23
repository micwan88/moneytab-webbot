package io.github.micwan88.moneytab.messaging;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

public class TelegramBot {
	public static final String URL_TELEGRAM_BOT_BASE = "https://api.telegram.org/bot";
	public static final String URL_TELEGRAM_BOT_SENDMSG_CMD = "/sendMessage";
	
	public static final String TELEGRAM_BOT_SENDMSG_PARAM_CHATID = "chat_id";
	public static final String TELEGRAM_BOT_SENDMSG_PARAM_MSGTEXT = "text";
	public static final String TELEGRAM_BOT_SENDMSG_PARAM_PARSE_MODE = "parse_mode";
	public static final int TELEGRAM_BOT_SENDMSG_MAXLENGTH = 4096;
	
	public static final String TELEGRAM_BOT_SENDMSG_VALUE_PARSE_MODE_HTML = "HTML";
	
	private static final Logger myLogger = LogManager.getLogger(TelegramBot.class);
	
	private String tgBotToken = null;
	
	private String tgBotChatID = null;
	
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

	public TelegramBot(String tgBotToken, String tgBotChatID) {
		this.tgBotToken = tgBotToken;
		this.tgBotChatID = tgBotChatID;
	}

	public int postNotification(String notificationMsg) {
		String apiURL = URL_TELEGRAM_BOT_BASE + tgBotToken + URL_TELEGRAM_BOT_SENDMSG_CMD;
		
		String postMsg = filterTgRestrictedKeywords(notificationMsg);
		if (postMsg.length() > TELEGRAM_BOT_SENDMSG_MAXLENGTH)
			postMsg = notificationMsg.substring(0, TELEGRAM_BOT_SENDMSG_MAXLENGTH);
		
		myLogger.debug("Start postNotification URL: {}", apiURL);
		myLogger.debug("postMsg: {}", postMsg);
		
		JsonObject outputJson = new JsonObject();
		outputJson.addProperty(TELEGRAM_BOT_SENDMSG_PARAM_CHATID, tgBotChatID);
		outputJson.addProperty(TELEGRAM_BOT_SENDMSG_PARAM_MSGTEXT, postMsg);
		outputJson.addProperty(TELEGRAM_BOT_SENDMSG_PARAM_PARSE_MODE, TELEGRAM_BOT_SENDMSG_VALUE_PARSE_MODE_HTML);
		
		try {
			String responseMsg = Request.Post(apiURL).bodyString(postMsg, ContentType.APPLICATION_JSON).execute().returnContent().asString();
			if (responseMsg != null && responseMsg.matches("\"ok\"\\s*\\:\\s*true")) {
				myLogger.debug("postNotification done");
				return 0;
			}
			myLogger.debug("postNotification error - responseMsg: {}", responseMsg);
		} catch (ClientProtocolException e) {
			myLogger.error("Cannot execute http request", e);
		} catch (IOException e) {
			myLogger.error("Cannot execute http request", e);
		} catch (Exception e) {
			myLogger.error("Unexpected error", e);
		}
		return -1;
	}
	
	private String filterTgRestrictedKeywords(String sourceString) {
		return sourceString.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
}
