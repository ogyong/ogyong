package com.nribeka.ogyong.listener;

/**
 */
public interface LocationSelectionListener {
    void onRandomLocation(int destination, boolean checked);

    void onIncludeLocation(int destination, boolean checked);
}
