package org.fox.ttrss;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.fox.ttrss.types.Article;
import org.junit.Test;
import org.mockito.MockedStatic;

public class EmptyHeadlineNavigationTest {
    private Uri uri(String value, String scheme, String host) {
        Uri uri = mock(Uri.class);
        when(uri.toString()).thenReturn(value);
        when(uri.getScheme()).thenReturn(scheme);
        when(uri.getHost()).thenReturn(host);
        return uri;
    }

    @Test
    public void emptyLinksDoNotReachBrowserPreferencesOrLaunchAnActivity() {
        CommonActivity activity = mock(CommonActivity.class, CALLS_REAL_METHODS);
        activity.m_prefs = mock(SharedPreferences.class);

        activity.openUri(null);
        activity.openUri(uri("", null, null));
        activity.openUri(uri("  ", null, null));
        activity.openUri(uri("https:", "https", null));
        activity.openUri(uri("http://", "http", ""));

        verifyNoInteractions(activity.m_prefs);
        verify(activity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void validWebLinksStillOpen() {
        assertOpens(uri("https://example.org/article", "https", "example.org"));
    }

    @Test
    public void nonWebSchemesDoNotRequireAHost() {
        assertOpens(uri("mailto:reader@example.org", "mailto", null));
    }

    private void assertOpens(Uri uri) {
        CommonActivity activity = mock(CommonActivity.class, CALLS_REAL_METHODS);
        activity.m_prefs = mock(SharedPreferences.class);
        when(activity.m_prefs.getString("preferred_browser", "")).thenReturn("org.example.browser");
        doNothing().when(activity).startActivity(any(Intent.class));

        activity.openUri(uri);

        verify(activity).startActivity(any(Intent.class));
    }

    @Test
    public void footerCannotBecomeTheActiveArticleOrOpenTheBrowser() {
        MasterActivity master = mock(MasterActivity.class, CALLS_REAL_METHODS);
        DetailActivity detail = mock(DetailActivity.class, CALLS_REAL_METHODS);
        Article footer = new Article(Article.TYPE_AMR_FOOTER);

        try (MockedStatic<Application> application = mockStatic(Application.class)) {
            master.onArticleSelected(footer);
            detail.onArticleSelected(footer);
            master.onArticleSelected(null);
            detail.onArticleSelected(null);

            application.verifyNoInteractions();
            verify(master, never()).openUri(any());
            verify(master, never()).startActivity(any(Intent.class));
            verify(detail, never()).openUri(any());
        }
    }
}
