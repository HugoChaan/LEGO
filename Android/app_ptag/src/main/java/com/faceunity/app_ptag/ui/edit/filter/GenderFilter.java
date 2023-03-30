package com.faceunity.app_ptag.ui.edit.filter;


import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 2021/9/7 0007 14:15.


 * 性别筛选的规则。
 */
public class GenderFilter implements FilterRule {
    private List<GenderFilterKey> filterKeyList;

    private GenderFilter() {
        this.filterKeyList = new ArrayList<>();
    }

    public GenderFilter(List<GenderFilterKey> filterKeyList) {
        this.filterKeyList = filterKeyList;
    }

    public GenderFilter(GenderFilterKey... keys) {
        this.filterKeyList = Arrays.asList(keys);
    }

    @Override
    @Nullable
    public GenderFilterKey filter(Object object) {

        return null;
    }



    @Override
    public boolean isConform(FilterKey filterKey) {
        if (filterKey == null) return true;
        if (!(filterKey instanceof GenderFilterKey)) return true;
        GenderFilterKey genderFilterKey = (GenderFilterKey) filterKey;
        if (filterKeyList == null) return true;
        if (filterKeyList.contains(genderFilterKey)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String tag() {
        return "Gender";
    }

    public enum GenderFilterKey implements FilterKey {
        JustMale, JustFemale, All
    }
}
