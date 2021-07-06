package aq.metallists.loudbang;

import android.app.Application;
import android.content.Context;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

@AcraCore(buildConfigClass = BuildConfig.class)
public class LoudBangApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this, LoudBangApplication.getAcraBuilder(this));
    }

    public static CoreConfigurationBuilder getAcraBuilder(Context ctx) {
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(ctx);
        builder.setBuildConfigClass(BuildConfig.class).setReportFormat(StringFormat.JSON);
        builder.getPluginConfigurationBuilder(DialogConfigurationBuilder.class)
                .setResText(R.string.acra_sendmail_required)
                .setEnabled(true);
        builder.getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class)
                .setMailTo("themetallists@freemail.hu")
                .setSubject("ACRA ERROR REPORT")
                .setReportAsFile(false)
                .setEnabled(true);

        return builder;
    }

}
