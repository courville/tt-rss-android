package org.fox.ttrss.util;

import android.content.Context;
import android.util.AttributeSet;

import androidx.activity.BackEventCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.navigation.NavigationView;

/** Routes Material's predictive Back callback through the activity's category navigation. */
public class CategoryNavigationView extends NavigationView {
    private Runnable m_onBackInvoked;

    public CategoryNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnBackInvokedListener(Runnable listener) {
        m_onBackInvoked = listener;
    }

    @Override
    public void handleBackInvoked() {
        if (m_onBackInvoked != null) m_onBackInvoked.run();
    }

    // Back navigates within categories or leaves the activity; it does not close
    // this drawer, so Material's drawer-closing preview would show the wrong action.
    @Override
    public void startBackProgress(@NonNull BackEventCompat backEvent) {
    }

    @Override
    public void updateBackProgress(@NonNull BackEventCompat backEvent) {
    }

    @Override
    public void cancelBackProgress() {
    }
}
