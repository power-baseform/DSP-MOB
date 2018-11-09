package com.baseform.power;

import android.graphics.drawable.Drawable;

public class DeploymentItem  {
    String url;
    String name;
    Drawable icon;

    DeploymentItem(String url, String name, Drawable icon) {
        this.url = url;
        this.name = name;
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public Drawable getIcon() {
        return icon;
    }
}
