package org.fox.ttrss;

import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import java.util.function.BooleanSupplier;

/** Intercepts Back only when it needs to reveal the category drawer. */
final class CategoryBackCallback extends OnBackPressedCallback implements
        DrawerLayout.DrawerListener, FragmentManager.OnBackStackChangedListener {
    private final DrawerLayout m_drawer;
    private final FragmentManager m_fragments;
    private final BooleanSupplier m_hasActiveFeed;
    private boolean m_drawerMoving;

    CategoryBackCallback(DrawerLayout drawer, FragmentManager fragments, BooleanSupplier hasActiveFeed) {
        super(false);
        m_drawer = drawer;
        m_fragments = fragments;
        m_hasActiveFeed = hasActiveFeed;
        if (drawer != null) drawer.addDrawerListener(this);
        fragments.addOnBackStackChangedListener(this);
        updateEnabled();
    }

    void updateEnabled() {
        // Decide before the gesture starts. With categories visible, FragmentManager
        // owns Back while it has history; at the root Android owns Back and its preview.
        setEnabled(m_drawer != null && !m_drawerMoving &&
                !m_drawer.isDrawerVisible(GravityCompat.START) &&
                (m_hasActiveFeed.getAsBoolean() || m_fragments.getBackStackEntryCount() > 0));
    }

    @Override
    public void handleOnBackPressed() {
        setEnabled(false);
        m_drawer.openDrawer(GravityCompat.START);
    }

    @Override
    public void onBackStackChanged() {
        updateEnabled();
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        updateEnabled();
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        updateEnabled();
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        updateEnabled();
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        m_drawerMoving = newState != DrawerLayout.STATE_IDLE;
        updateEnabled();
    }

    void dispose() {
        if (m_drawer != null) m_drawer.removeDrawerListener(this);
        m_fragments.removeOnBackStackChangedListener(this);
        remove();
    }
}
