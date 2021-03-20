package aq.metallists.loudbang.cutil;

import android.content.Context;

import aq.metallists.loudbang.R;

public class Bandplan {
    double dialfreq;
    String name;

    private Bandplan(String name, double dialfreq) {
        this.dialfreq = dialfreq;
        this.name = name;
    }

    public static Bandplan[] getBandPlan(Context ctx) {
        return new Bandplan[]{
                new Bandplan(ctx.getString(R.string.bandplan_b33400m), 0.0072),
                new Bandplan(ctx.getString(R.string.bandplan_b2200m), 0.136),
                new Bandplan(ctx.getString(R.string.bandplan_b600m), 0.4742),
                new Bandplan(ctx.getString(R.string.bandplan_b160m), 1.8366),
                new Bandplan(ctx.getString(R.string.bandplan_b80m), 3.5926),
                new Bandplan(ctx.getString(R.string.bandplan_b80m_jp), 3.5686),
                new Bandplan(ctx.getString(R.string.bandplan_b60m), 5.2872),
                new Bandplan(ctx.getString(R.string.bandplan_b60m_eu), 5.3647),
                new Bandplan(ctx.getString(R.string.bandplan_b40m), 7.0386),
                new Bandplan(ctx.getString(R.string.bandplan_b30m), 10.1387),
                new Bandplan(ctx.getString(R.string.bandplan_b20m), 14.0956),
                new Bandplan(ctx.getString(R.string.bandplan_b17m), 18.1046),
                new Bandplan(ctx.getString(R.string.bandplan_b15m), 21.0946),
                new Bandplan(ctx.getString(R.string.bandplan_b12m), 24.9246),
                new Bandplan(ctx.getString(R.string.bandplan_b10m), 28.1246),
                new Bandplan(ctx.getString(R.string.bandplan_b6m), 50.293),
                new Bandplan(ctx.getString(R.string.bandplan_b4m), 70.091),
                new Bandplan(ctx.getString(R.string.bandplan_b2m), 144.489),
                new Bandplan(ctx.getString(R.string.bandplan_b70cm), 432.300),
                new Bandplan(ctx.getString(R.string.bandplan_b23cm), 1296.500),
        };
    }
}
