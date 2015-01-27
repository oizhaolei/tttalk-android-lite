package com.ruptech.tttalk_android.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.fragment.SettingsFragment;

import butterknife.ButterKnife;

public class SettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.inject(this);

        String title = getString(R.string.action_settings);
        getSupportActionBar().setTitle(title);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.sample_content_fragment, SettingsFragment.newInstance());
            transaction.commit();
        }
    }
}
