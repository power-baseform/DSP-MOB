package com.baseform.power;

import android.content.ClipData;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initBindings();
    }


    private void initBindings() {
        ListView listView = findViewById(R.id.listView);

        List<DeploymentItem> items = new ArrayList<>();
        items.add(new DeploymentItem("https://leicester.power-h2020.eu/", "Leicester", getDrawable(R.drawable.lobby_leicester)));
        items.add(new DeploymentItem("https://sabadell.power-h2020.eu/", "Sabadell", getDrawable(R.drawable.lobby_sabadell)));
        items.add(new DeploymentItem("https://jerusalem.power-h2020.eu/", "Jerusalem", getDrawable(R.drawable.lobby_jerusalem)));
        items.add(new DeploymentItem("https://milton-keynes.power-h2020.eu/", "Milton Keynes", getDrawable(R.drawable.lobby_mk)));

        DeploymentAdapter customAdapter = new DeploymentAdapter(this, R.layout.deployment, items);
        listView.setAdapter(customAdapter);
    }
}
