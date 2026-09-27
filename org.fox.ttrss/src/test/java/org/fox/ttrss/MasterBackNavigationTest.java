package org.fox.ttrss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.view.KeyEvent;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import org.fox.ttrss.types.Feed;
import org.fox.ttrss.util.CategoryDrawerLayout;
import org.fox.ttrss.util.CategoryNavigationView;
import org.junit.Test;

public class MasterBackNavigationTest {
    private static class Navigation {
        final MasterActivity activity = mock(MasterActivity.class, CALLS_REAL_METHODS);
        final DrawerLayout drawer = mock(DrawerLayout.class);
        final FragmentManager fragments = mock(FragmentManager.class);
        final Runnable leaveActivity = mock(Runnable.class);
        final OnBackPressedDispatcher dispatcher = new OnBackPressedDispatcher(leaveActivity);
        final OnBackPressedCallback callback;
        boolean drawerVisible;
        int categoryDepth;

        Navigation(boolean hasDrawer, boolean hasFeed, int depth) {
            categoryDepth = depth;
            doReturn(fragments).when(activity).getSupportFragmentManager();
            doReturn(dispatcher).when(activity).getOnBackPressedDispatcher();
            doReturn(hasFeed ? new Feed(1, "Feed", false) : null).when(activity).getActiveFeed();
            when(fragments.getBackStackEntryCount()).thenAnswer(call -> categoryDepth);
            when(drawer.isDrawerVisible(GravityCompat.START)).thenAnswer(call -> drawerVisible);
            doAnswer(call -> {
                drawerVisible = true;
                return null;
            }).when(drawer).openDrawer(GravityCompat.START);

            // Model FragmentManager's lower-priority callback: each Back pops one
            // category, and an empty stack delegates to the activity fallback.
            dispatcher.addCallback(new OnBackPressedCallback(depth > 0) {
                @Override
                public void handleOnBackPressed() {
                    categoryDepth--;
                    setEnabled(categoryDepth > 0);
                }
            });
            callback = activity.createBackCallback(hasDrawer ? drawer : null);
            dispatcher.addCallback(callback);
        }
    }

    @Test
    public void headlinesThenRootDrawerThenLeaveActivity() {
        Navigation navigation = new Navigation(true, true, 0);

        navigation.dispatcher.onBackPressed();
        assertTrue(navigation.drawerVisible);
        verifyNoInteractions(navigation.leaveActivity);

        navigation.dispatcher.onBackPressed();
        verify(navigation.leaveActivity).run();
        verify(navigation.drawer, never()).closeDrawers();
    }

    @Test
    public void nestedCategoriesPopBeforeLeavingAndCallbackKeepsWorking() {
        Navigation navigation = new Navigation(true, true, 2);

        navigation.dispatcher.onBackPressed();
        assertEquals(2, navigation.categoryDepth);
        navigation.dispatcher.onBackPressed();
        assertEquals(1, navigation.categoryDepth);
        assertTrue(navigation.callback.isEnabled());
        navigation.dispatcher.onBackPressed();
        assertEquals(0, navigation.categoryDepth);
        verifyNoInteractions(navigation.leaveActivity);

        // Selecting a feed closes the drawer; Back must still reopen it after a pop.
        navigation.drawerVisible = false;
        navigation.dispatcher.onBackPressed();
        assertTrue(navigation.drawerVisible);
        verifyNoInteractions(navigation.leaveActivity);
        navigation.dispatcher.onBackPressed();
        verify(navigation.leaveActivity).run();
    }

    @Test
    public void initiallyOpenRootDrawerLeavesOnFirstBack() {
        Navigation navigation = new Navigation(true, false, 0);
        navigation.drawerVisible = true;

        navigation.dispatcher.onBackPressed();

        verify(navigation.leaveActivity).run();
    }

    @Test
    public void emptyMainScreenLeavesWithoutOpeningDrawer() {
        Navigation navigation = new Navigation(true, false, 0);

        navigation.dispatcher.onBackPressed();

        assertFalse(navigation.drawerVisible);
        verify(navigation.leaveActivity).run();
    }

    @Test
    public void tabletWithoutDrawerPopsCategoryThenLeaves() {
        Navigation navigation = new Navigation(false, true, 1);

        navigation.dispatcher.onBackPressed();
        assertEquals(0, navigation.categoryDepth);
        verifyNoInteractions(navigation.leaveActivity);
        navigation.dispatcher.onBackPressed();

        verify(navigation.leaveActivity).run();
        verifyNoInteractions(navigation.drawer);
    }

    @Test
    public void legacyBackIsNotConsumedByDrawer() {
        CategoryDrawerLayout drawer = mock(CategoryDrawerLayout.class, CALLS_REAL_METHODS);
        KeyEvent event = mock(KeyEvent.class);

        assertFalse(drawer.onKeyDown(KeyEvent.KEYCODE_BACK, event));
        assertFalse(drawer.onKeyUp(KeyEvent.KEYCODE_BACK, event));

        verifyNoInteractions(event);
        verify(drawer, never()).closeDrawers();
    }

    @Test
    public void predictiveBackNavigatesOnlyWhenCommitted() {
        Navigation navigation = new Navigation(true, true, 1);
        navigation.drawerVisible = true;
        CategoryNavigationView view = mock(CategoryNavigationView.class, CALLS_REAL_METHODS);
        view.setOnBackInvokedListener(navigation.dispatcher::onBackPressed);
        BackEventCompat event = mock(BackEventCompat.class);

        view.startBackProgress(event);
        view.updateBackProgress(event);
        view.cancelBackProgress();
        assertEquals(1, navigation.categoryDepth);
        verifyNoInteractions(navigation.leaveActivity);

        view.handleBackInvoked();
        assertEquals(0, navigation.categoryDepth);
        assertTrue(navigation.drawerVisible);
        view.handleBackInvoked();
        verify(navigation.leaveActivity).run();
    }
}
