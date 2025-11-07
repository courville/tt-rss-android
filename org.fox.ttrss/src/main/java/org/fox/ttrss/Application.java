package org.fox.ttrss;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.google.android.material.color.DynamicColors;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.fox.ttrss.types.Article;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class Application extends android.app.Application {

    private static Application m_singleton;

    private String m_sessionId;
    private int m_apiLevel;
    public LinkedHashMap<String, String> m_customSortModes = new LinkedHashMap<>();
    ConnectivityManager m_cmgr;
    ArticleModel m_articleModel;

    public static Application getInstance() {
        return m_singleton;
    }

    public static List<Article> getArticles() {
        return getInstance().m_articleModel.getArticles().getValue();
    }

    public static ArticleModel getArticlesModel() {
        return getInstance().m_articleModel;
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        DynamicColors.applyToActivitiesIfAvailable(this);

        m_singleton = this;
        m_cmgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        m_articleModel = new ArticleModel(this);
    }

    public String getSessionId() {
        return m_sessionId;
    }

    public void setSessionId(String sessionId) {
        m_sessionId = sessionId;
    }

    public int getApiLevel() {
        return m_apiLevel;
    }

    public void setApiLevel(int apiLevel) {
        m_apiLevel = apiLevel;
    }

    public void save(Bundle out) {

        out.setClassLoader(getClass().getClassLoader());
        out.putString("gs:sessionId", m_sessionId);
        out.putInt("gs:apiLevel", m_apiLevel);
        out.putSerializable("gs:customSortTypes", m_customSortModes);
    }

    /**
     * @noinspection unchecked
     */
    public void load(Bundle in) {
        if (in != null) {
            m_sessionId = in.getString("gs:sessionId");
            m_apiLevel = in.getInt("gs:apiLevel");

            HashMap<String, String> tmp = (HashMap<String, String>) in.getSerializable("gs:customSortTypes");

            m_customSortModes.clear();
            m_customSortModes.putAll(tmp);
        }
    }

    public boolean isWifiConnected() {
        NetworkInfo wifi = m_cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (wifi != null)
            return wifi.isConnected();

        return false;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        if (!BuildConfig.DEBUG)
            ACRA.init(this, new CoreConfigurationBuilder()
                    .withBuildConfigClass(BuildConfig.class)
                    .withReportContent(ReportField.APP_VERSION_NAME, ReportField.APP_VERSION_CODE, ReportField.STACK_TRACE)
                    .withReportFormat(StringFormat.KEY_VALUE_LIST)
                    .withPluginConfigurations(
                            new DialogConfigurationBuilder()
                                    .withText(getString(R.string.crash_dialog_text_email))
                                    .withResTheme(R.style.Theme_AppCompat_Dialog)
                                    .build(),
                            new MailSenderConfigurationBuilder()
                                    .withMailTo("software+ttrss-acra@courville.org")
                                    .withReportAsFile(true)
                                    .withReportFileName("crash.txt")
                                    .build()
                    )
                    .build());
    }
}
