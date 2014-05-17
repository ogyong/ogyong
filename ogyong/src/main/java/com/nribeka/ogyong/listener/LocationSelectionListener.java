package com.nribeka.ogyong.listener;

/**
 */
public interface LocationSelectionListener {
    void onRandomLocation(int source, boolean checked);

    void onIncludeLocation(int source, boolean checked);
}
