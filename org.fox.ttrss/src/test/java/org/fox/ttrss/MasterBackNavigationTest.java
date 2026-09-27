package org.fox.ttrss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.view.KeyEvent;
import android.view.View;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import org.fox.ttrss.util.CategoryDrawerLayout;
import org.junit.Test;

public class MasterBackNavigationTest {
    private static class Navigation {
        final DrawerLayout drawer = mock(DrawerLayout.class);
        final FragmentManager fragments = mock(FragmentManager.class);
        final Runnable leaveActivity = mock(Runnable.class);
        final OnBackPressedDispatcher dispatcher = new OnBackPressedDispatcher(leaveActivity);
        final CategoryBackCallback callback;
        final OnBackPressedCallback fragmentCallback;
        boolean drawerVisible;
        boolean hasFeed;
        int categoryDepth;

        Navigation(boolean hasDrawer, boolean hasFeed, int depth) {
            categoryDepth = depth;
            this.hasFeed = hasFeed;
            when(fragments.getBackStackEntryCount()).thenAnswer(call -> categoryDepth);
            when(drawer.isDrawerVisible(GravityCompat.START)).thenAnswer(call -> drawerVisible);
            doAnswer(call -> {
                drawerVisible = true;
                return null;
            }).when(drawer).openDrawer(GravityCompat.START);

            // Model FragmentManager's lower-priority callback: each Back pops one
            // category, and an empty stack delegates to the activity fallback.
            fragmentCallback = new OnBackPressedCallback(depth > 0) {
                @Override
                public void handleOnBackPressed() {
                    categoryDepth--;
                    setEnabled(categoryDepth > 0);
                }
            };
            dispatcher.addCallback(fragmentCallback);
            callback = new CategoryBackCallback(hasDrawer ? drawer : null, fragments, () -> this.hasFeed);
            dispatcher.addCallback(callback);
        }

        void setDrawerVisible(boolean visible) {
            drawerVisible = visible;
            if (visible) callback.onDrawerOpened(null);
            else callback.onDrawerClosed(null);
        }
    }

    @Test
    public void headlinesThenRootDrawerThenLeaveActivity() {
        Navigation navigation = new Navigation(true, true, 0);

        navigation.dispatcher.onBackPressed();
        assertTrue(navigation.drawerVisible);
        assertFalse(navigation.dispatcher.hasEnabledCallbacks());
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
        assertFalse(navigation.callback.isEnabled());
        navigation.dispatcher.onBackPressed();
        assertEquals(0, navigation.categoryDepth);
        assertFalse(navigation.dispatcher.hasEnabledCallbacks());
        verifyNoInteractions(navigation.leaveActivity);

        // Selecting a feed closes the drawer; Back must still reopen it after a pop.
        navigation.setDrawerVisible(false);
        assertTrue(navigation.callback.isEnabled());
        navigation.dispatcher.onBackPressed();
        assertTrue(navigation.drawerVisible);
        verifyNoInteractions(navigation.leaveActivity);
        navigation.dispatcher.onBackPressed();
        verify(navigation.leaveActivity).run();
    }

    @Test
    public void initiallyOpenRootDrawerLeavesOnFirstBack() {
        Navigation navigation = new Navigation(true, false, 0);
        navigation.setDrawerVisible(true);
        assertFalse(navigation.dispatcher.hasEnabledCallbacks());

        navigation.dispatcher.onBackPressed();

        verify(navigation.leaveActivity).run();
    }

    @Test
    public void emptyMainScreenLeavesWithoutOpeningDrawer() {
        Navigation navigation = new Navigation(true, false, 0);
        assertFalse(navigation.dispatcher.hasEnabledCallbacks());

        navigation.dispatcher.onBackPressed();

        assertFalse(navigation.drawerVisible);
        verify(navigation.leaveActivity).run();
    }

    @Test
    public void tabletWithoutDrawerPopsCategoryThenLeaves() {
        Navigation navigation = new Navigation(false, true, 1);
        assertFalse(navigation.callback.isEnabled());

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
    public void cancelledGestureDoesNotOpenDrawerOrChangeCategories() {
        Navigation navigation = new Navigation(true, true, 1);
        BackEventCompat event = mock(BackEventCompat.class);

        navigation.callback.handleOnBackStarted(event);
        navigation.callback.handleOnBackProgressed(event);
        navigation.callback.handleOnBackCancelled();
        assertEquals(1, navigation.categoryDepth);
        assertFalse(navigation.drawerVisible);
        assertTrue(navigation.callback.isEnabled());
        verifyNoInteractions(navigation.leaveActivity);
    }

    @Test
    public void openCategoryDrawerLeavesFragmentManagerInChargeBeforeGesture() {
        Navigation navigation = new Navigation(true, true, 1);
        navigation.setDrawerVisible(true);

        assertFalse(navigation.callback.isEnabled());
        assertTrue(navigation.fragmentCallback.isEnabled());
        navigation.dispatcher.onBackPressed();

        assertFalse(navigation.dispatcher.hasEnabledCallbacks());
        verifyNoInteractions(navigation.leaveActivity);
    }

    @Test
    public void closingDrawerAndSelectingFeedReenablesDrawerNavigation() {
        Navigation navigation = new Navigation(true, false, 0);
        navigation.setDrawerVisible(true);
        navigation.hasFeed = true;
        navigation.callback.updateEnabled();
        assertFalse(navigation.callback.isEnabled());

        navigation.callback.onDrawerStateChanged(DrawerLayout.STATE_SETTLING);
        navigation.setDrawerVisible(false);
        assertFalse(navigation.callback.isEnabled());
        navigation.callback.onDrawerStateChanged(DrawerLayout.STATE_IDLE);
        assertTrue(navigation.callback.isEnabled());
    }

    @Test
    public void backStackChangesUpdateDrawerNavigationBeforeNextGesture() {
        Navigation navigation = new Navigation(true, false, 0);
        assertFalse(navigation.callback.isEnabled());

        navigation.categoryDepth = 1;
        navigation.callback.onBackStackChanged();
        assertTrue(navigation.callback.isEnabled());

        navigation.categoryDepth = 0;
        navigation.callback.onBackStackChanged();
        assertFalse(navigation.callback.isEnabled());
    }

    @Test
    public void navigationContainerCannotRegisterPredictiveDrawerClosingCallback() {
        CategoryDrawerLayout drawer = mock(CategoryDrawerLayout.class, CALLS_REAL_METHODS);
        View container = mock(View.class);
        when(container.getId()).thenReturn(R.id.modal_navigation_view);

        assertNull(drawer.findOnBackInvokedDispatcherForChild(container, container));
    }

    @Test
    public void disposeRemovesStateListenersAndCallback() {
        Navigation navigation = new Navigation(true, true, 0);

        navigation.callback.dispose();

        assertFalse(navigation.dispatcher.hasEnabledCallbacks());
        verify(navigation.drawer).removeDrawerListener(navigation.callback);
        verify(navigation.fragments).removeOnBackStackChangedListener(navigation.callback);
    }
}
