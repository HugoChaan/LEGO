package com.faceunity.app_ptag.ui.edit.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class FilterGroup {
    private final List<FilterRule> filterRuleList = new ArrayList<>();

    public void replaceSameTagRule(FilterRule filterRule) {
        Iterator<FilterRule> iterator = filterRuleList.iterator();
        while (iterator.hasNext()) {
            FilterRule rule = iterator.next();
            if (!filterRule.tag().isEmpty() && rule.tag().equals(filterRule.tag())) {
                iterator.remove();
            }
        }
        filterRuleList.add(filterRule);
    }

    public void addFilterRule(FilterRule filterRule) {
        filterRuleList.add(filterRule);
    }

    public void clearFilterRule() {
        filterRuleList.clear();
    }

    public void replaceFilterGroup(FilterGroup filterGroup) {
        clearFilterRule();
        for (FilterRule filterRule : filterGroup.filterRuleList) {
            addFilterRule(filterRule);
        }
    }

    public <T> List<T> filter(List<T> originList) {
        if (originList == null) return originList;
        if (filterRuleList.size() == 0) return originList;
        List<T> ret = new ArrayList<>();
        for (T item : originList) {
            if (isConform(item)) {
                ret.add(item);
            }
        }
        return ret;
    }

    public <T> Set<T> filter(Set<T> originList) {
        if (filterRuleList.size() == 0) return originList;
        Set<T> ret = new HashSet<>();
        for (T item : originList) {
            if (isConform(item)) {
                ret.add(item);
            }
        }
        return ret;
    }

    private <T> boolean isConform(T t) {
        for (FilterRule filterRule : filterRuleList) {
            FilterRule.FilterKey filterKey = filterRule.filter(t);
            if (filterKey == null) continue;
            boolean nowRuleIsConform = filterRule.isConform(filterKey);
            if (!nowRuleIsConform) {
                return false;
            }
        }
        return true;
    }
}
