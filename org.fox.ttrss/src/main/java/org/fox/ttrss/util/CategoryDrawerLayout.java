package org.fox.ttrss.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

/** Leaves Back navigation to the activity instead of closing the category drawer. */
@SuppressLint("GestureBackNavigation") // Only bypasses legacy interception; navigation uses AndroidX.
public class CategoryDrawerLayout extends DrawerLayout {
    public CategoryDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // On older Android versions Back travels through the view hierarchy first.
        if (keyCode == KeyEvent.KEYCODE_BACK) return false;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return false;
        return super.onKeyUp(keyCode, event);
    }
}
