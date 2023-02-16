package io.github.micwan88.moneytab.bean;

public class NotificationItem {
	private String type = null;
	
	private String dateInString = null;
	
	private String title = null;
	
	private String fullDescription = null;
	
	private String pageLink = null;
	
	private String videoLink = null;
	
	private String checksum = null;
	
	private boolean isSent = false;
	private boolean gotError = false;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDateInString() {
		return dateInString;
	}

	public void setDateInString(String dateInString) {
		this.dateInString = dateInString;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getFullDescription() {
		return fullDescription;
	}

	public void setFullDescription(String fullDescription) {
		this.fullDescription = fullDescription;
	}

	public String getPageLink() {
		return pageLink;
	}

	public void setPageLink(String pageLink) {
		this.pageLink = pageLink;
	}

	public String getVideoLink() {
		return videoLink;
	}

	public void setVideoLink(String videoLink) {
		this.videoLink = videoLink;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public boolean isSent() {
		return isSent;
	}

	public void setSent(boolean isSent) {
		this.isSent = isSent;
	}

	public boolean isGotError() {
		return gotError;
	}

	public void setGotError(boolean gotError) {
		this.gotError = gotError;
	}

	public NotificationItem() {
	}

	public NotificationItem(String type, String dateInString, String title, String fullDescription) {
		this.type = type;
		this.dateInString = dateInString;
		this.title = title;
		this.fullDescription = fullDescription;
	}

	public NotificationItem(String type, String dateInString, String title, String fullDescription, String pageLink, String videoLink) {
		this(type, dateInString, title, fullDescription);
		this.pageLink = pageLink;
		this.videoLink = videoLink;
	}

	@Override
	public String toString() {
		return "NotificationItem [type=" + type + ", dateInString=" + dateInString + ", title=" + title
				+ ", fullDescription=" + fullDescription + ", pageLink=" + pageLink + ", videoLink=" + videoLink
				+ ", checksum=" + checksum + ", isSent=" + isSent + ", gotError=" + gotError + "]";
	}
}
