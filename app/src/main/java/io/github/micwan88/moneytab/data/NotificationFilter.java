package io.github.micwan88.moneytab.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import io.github.micwan88.moneytab.bean.NotificationItem;

public class NotificationFilter {
	private HashSet<String> filterItemSet = new HashSet<>();
	
	private boolean isBlackListFilter = true;

	public HashSet<String> getFilterItemSet() {
		return filterItemSet;
	}

	public void setFilterItemSet(HashSet<String> filterItemSet) {
		this.filterItemSet = filterItemSet;
	}

	public boolean isBlackListFilter() {
		return isBlackListFilter;
	}

	public void setBlackListFilter(boolean isBlackListFilter) {
		this.isBlackListFilter = isBlackListFilter;
	}

	public NotificationFilter(String filterString) {
		if (filterString != null) {
			if (filterString.startsWith("^")) {
				filterString = filterString.substring(1);
			} else
				isBlackListFilter = false;
			
			String[] filterValueArray = filterString.split(",");
			Arrays.stream(filterValueArray).forEach((filterValue) -> filterItemSet.add(filterValue));
		}
	}
	
	public NotificationFilter(List<String> filterStringInList, boolean isBlackListFilter) {
		if (filterStringInList != null) {
			filterItemSet.addAll(filterStringInList);
		}
		
		this.isBlackListFilter = isBlackListFilter;
	}
	
	public boolean filterDate(NotificationItem notificationItem) {
		boolean result = isBlackListFilter ^ filterItemSet.contains(notificationItem.getDateInString());
		return result;
	}
	
	public boolean filterTitle(NotificationItem notificationItem) {
		boolean result = isBlackListFilter ^ filterItemSet.contains(notificationItem.getTitle());
		return result;
	}
	
	public boolean filterChecksum(NotificationItem notificationItem) {
		boolean result = isBlackListFilter ^ filterItemSet.contains(notificationItem.getFullDescriptionChecksum());
		return result;
	}

	@Override
	public String toString() {
		return "NotificationFilter [filterItemSet=(" + filterItemSet.stream().collect(Collectors.joining(",")) + "), isBlackListFilter=" + isBlackListFilter + "]";
	}
}
