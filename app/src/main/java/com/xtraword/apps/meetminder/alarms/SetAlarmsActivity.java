package com.xtraword.apps.meetminder.alarms;

import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xtraword.apps.meetminder.BaseActivity;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.adapters.AlarmsAdapter;
import com.xtraword.apps.meetminder.databases.DatabaseHelperAlarms;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SetAlarmsActivity extends BaseActivity {
    private AlarmsAdapter adapter;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private DatabaseHelperAlarms dbHelper;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarms);
        this.recyclerView = (RecyclerView) findViewById(R.id.recyclerViewAlarms);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.dbHelper = new DatabaseHelperAlarms(this);
        loadAlarms();
    }

    private void loadAlarms() {
        List<DatabaseHelperAlarms.AlarmModel> alarms = this.dbHelper.getAllAlarms();
        if (alarms.isEmpty()) {
            Toast.makeText(this, "No alarms set", 0).show();
        }
        this.adapter = new AlarmsAdapter(this, alarms);
        this.recyclerView.setAdapter(this.adapter);
    }
}
