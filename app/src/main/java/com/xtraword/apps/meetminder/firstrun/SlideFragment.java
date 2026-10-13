package com.xtraword.apps.meetminder.firstrun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.fragment.app.Fragment;
import com.xtraword.apps.meetminder.R;

public class SlideFragment extends Fragment {
    private String buttonText;
    private String description;
    private String permission;
    private String title;

    public SlideFragment(String title, String description, String buttonText, String permission) {
        this.title = title;
        this.description = description;
        this.buttonText = buttonText;
        this.permission = permission;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slide, container, false);
        TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
        TextView tvDescription = (TextView) view.findViewById(R.id.tvDescription);
        Button btnRequestPermission = (Button) view.findViewById(R.id.btnRequestPermission);
        tvTitle.setText(this.title);
        tvDescription.setText(this.description);
        btnRequestPermission.setText(this.buttonText);
        btnRequestPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                SlideFragment.this.m307x625877cc(view2);
            }
        });
        return view;
    }

    void m307x625877cc(View v) {
        requestPermission();
    }

    private void requestPermission() {
        requestPermissions(new String[]{this.permission}, TypedValues.TYPE_TARGET);
    }
}
