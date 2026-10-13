package com.xtraword.apps.meetminder.manager;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xtraword.apps.meetminder.BaseActivity;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;
import java.util.Iterator;
import java.util.List;

public class MeetingListActivity extends BaseActivity {
    private Button btnDeleteSelected;
    private DatabaseHelper dbHelper;
    private MeetingAdapterAll meetingAdapter;
    private RecyclerView meetingRecyclerView;
    TextView noMeetings;
    private CheckBox selectAllCheckBox;
    private CheckBox selectOngoindCheckBox;
    private CheckBox selectPastCheckBox;
    private CheckBox selectPendingCheckBox;
    List<com.xtraword.apps.meetminder.Meeting> updatedMeetings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_list);
        this.meetingRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        this.btnDeleteSelected = (Button) findViewById(R.id.btnDeleteSelected);
        this.noMeetings = (TextView) findViewById(R.id.noMeetings);
        this.selectAllCheckBox = (CheckBox) findViewById(R.id.selectAll);
        this.selectPendingCheckBox = (CheckBox) findViewById(R.id.selectPending);
        this.selectOngoindCheckBox = (CheckBox) findViewById(R.id.selectOngoing);
        this.selectPastCheckBox = (CheckBox) findViewById(R.id.selectPast);
        this.noMeetings.setVisibility(8);
        this.dbHelper = new DatabaseHelper(this);
        this.updatedMeetings = this.dbHelper.getAllMeetings();
        int listsize = this.updatedMeetings != null ? this.updatedMeetings.size() : 0;
        if (listsize == 0) {
            this.noMeetings.setVisibility(0);
        } else {
            this.noMeetings.setVisibility(8);
        }
        this.dbHelper = new DatabaseHelper(this);
        setupRecyclerView();
        this.btnDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MeetingListActivity.this.m349xcd53574f(view);
            }
        });
        this.selectAllCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MeetingListActivity.this.m350x5a406e6e(compoundButton, z);
            }
        });
        this.selectPendingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MeetingListActivity.this.m351xe72d858d(compoundButton, z);
            }
        });
        this.selectOngoindCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MeetingListActivity.this.m352x741a9cac(compoundButton, z);
            }
        });
        this.selectPastCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                MeetingListActivity.this.m353x107b3cb(compoundButton, z);
            }
        });
    }

    void m349xcd53574f(View view) {
        final List<com.xtraword.apps.meetminder.Meeting> selectedMeetings = this.meetingAdapter.getSelectedMeetings();
        if (selectedMeetings.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("No Selection").setMessage("Please select at least one meeting to delete.").setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        } else {
            new AlertDialog.Builder(this).setTitle("Delete Meetings").setMessage("Are you sure you want to delete the selected meetings?").setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MeetingListActivity.this.m348x40664030(selectedMeetings, dialogInterface, i);
                }
            }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null).show();
        }
    }

    void m348x40664030(List selectedMeetings, DialogInterface dialog, int which) {
        Iterator it = selectedMeetings.iterator();
        while (it.hasNext()) {
            com.xtraword.apps.meetminder.Meeting meeting = (com.xtraword.apps.meetminder.Meeting) it.next();
            this.dbHelper.deleteMeeting(meeting.getId());
        }
        refreshMeetingList();
    }

    void m350x5a406e6e(CompoundButton buttonView, boolean isChecked) {
        this.meetingAdapter.selectAll(isChecked);
        if (isChecked) {
            this.selectPendingCheckBox.setChecked(false);
            this.selectOngoindCheckBox.setChecked(false);
            this.selectPastCheckBox.setChecked(false);
        }
        this.selectPendingCheckBox.setEnabled(!isChecked);
        this.selectOngoindCheckBox.setEnabled(!isChecked);
        this.selectPastCheckBox.setEnabled(!isChecked);
    }

    void m351xe72d858d(CompoundButton buttonView, boolean isChecked) {
        this.meetingAdapter.selectAllPending(isChecked);
    }

    void m352x741a9cac(CompoundButton buttonView, boolean isChecked) {
        this.meetingAdapter.selectAllOngoing(isChecked);
    }

    void m353x107b3cb(CompoundButton buttonView, boolean isChecked) {
        this.meetingAdapter.selectAllPast(isChecked);
    }

    private void setupRecyclerView() {
        List<com.xtraword.apps.meetminder.Meeting> meetings = this.dbHelper.getAllMeetings();
        this.meetingAdapter = new MeetingAdapterAll(this, meetings);
        this.meetingRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.meetingRecyclerView.setAdapter(this.meetingAdapter);
    }

    private void refreshMeetingList() {
        this.dbHelper = new DatabaseHelper(this);
        this.updatedMeetings = this.dbHelper.getAllMeetings();
        this.meetingAdapter.updateData(this.updatedMeetings);
        this.meetingAdapter.notifyDataSetChanged();
        int listsize = this.updatedMeetings != null ? this.updatedMeetings.size() : 0;
        if (listsize == 0) {
            this.noMeetings.setVisibility(0);
        } else {
            this.noMeetings.setVisibility(8);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMeetingList();
        this.dbHelper = new DatabaseHelper(this);
        this.updatedMeetings = this.dbHelper.getAllMeetings();
        int listsize = this.updatedMeetings != null ? this.updatedMeetings.size() : 0;
        if (listsize == 0) {
            this.noMeetings.setVisibility(0);
        } else {
            this.noMeetings.setVisibility(8);
        }
    }
}
