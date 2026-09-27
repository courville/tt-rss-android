package org.fox.ttrss.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.drawerlayout.widget.DrawerLayout;

import org.fox.ttrss.R;

/** Leaves Back navigation to the activity instead of closing the category drawer. */
@SuppressLint("GestureBackNavigation") // Only bypasses legacy interception; navigation uses AndroidX.
public class CategoryDrawerLayout extends DrawerLayout {
    public CategoryDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Nullable
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressWarnings("ReferenceEquality") // Match the exact View instance, regardless of equals() overrides.
    @Override
    public OnBackInvokedDispatcher findOnBackInvokedDispatcherForChild(
            @NonNull View child, @NonNull View requester) {
        // Material's NavigationView must not register its drawer-closing overlay
        // callback: it would mask FragmentManager and the system's exit preview.
        // Only exclude the container itself; descendants keep their normal dispatchers.
        if (child == requester && requester.getId() == R.id.modal_navigation_view) return null;
        return super.findOnBackInvokedDispatcherForChild(child, requester);
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
