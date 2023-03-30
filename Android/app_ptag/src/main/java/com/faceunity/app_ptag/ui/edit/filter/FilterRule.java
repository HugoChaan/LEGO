package com.faceunity.app_ptag.ui.edit.filter;

import org.jetbrains.annotations.Nullable;

/**
 * Created on 2021/9/7 0007 14:03.


 */
public interface FilterRule {

    @Nullable
    FilterKey filter(Object object);

    boolean isConform(FilterKey filterKey);

    default String tag() {
        return "";
    }

    public interface FilterKey {

    }
}
