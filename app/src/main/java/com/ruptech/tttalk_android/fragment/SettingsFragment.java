package com.ruptech.tttalk_android.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.activity.MainActivity;
import com.ruptech.tttalk_android.model.ServerAppInfo;
import com.ruptech.tttalk_android.task.GenericTask;
import com.ruptech.tttalk_android.task.TaskAdapter;
import com.ruptech.tttalk_android.task.TaskListener;
import com.ruptech.tttalk_android.task.TaskResult;
import com.ruptech.tttalk_android.task.impl.VersionCheckTask;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class SettingsFragment extends Fragment implements OnClickListener,
        OnCheckedChangeListener {
    private final TaskListener serverInfoCheckTaskListener = new TaskAdapter() {

        @Override
        public void onPostExecute(GenericTask task, TaskResult result) {
            VersionCheckTask t = (VersionCheckTask) task;
            ServerAppInfo serverAppInfo = t.getServerAppInfo();
            if (serverAppInfo != null) {
                checkVersion(serverAppInfo);
            }
        }

        @Override
        public void onPreExecute(GenericTask task) {
        }

    };
    @InjectView(R.id.face)
    ImageView mHeadIcon;
    @InjectView(R.id.statusIcon)
    ImageView mStatusIcon;
    @InjectView(R.id.status)
    TextView mStatusView;
    @InjectView(R.id.nick)
    TextView mNickView;
    @InjectView(R.id.show_offline_roster_switch)
    CheckBox mShowOfflineRosterCheckBox;
    @InjectView(R.id.notify_run_background_switch)
    CheckBox mNotifyRunBackgroundCheckBox;
    @InjectView(R.id.new_msg_sound_switch)
    CheckBox mNewMsgSoundCheckBox;
    @InjectView(R.id.new_msg_vibrator_switch)
    CheckBox mNewMsgVibratorCheckBox;
    @InjectView(R.id.new_msg_led_switch)
    CheckBox mNewMsgLedCheckBox;
    @InjectView(R.id.visiable_new_msg_switch)
    CheckBox mVisiableNewMsgCheckBox;
    @InjectView(R.id.show_head_switch)
    CheckBox mShowHeadCheckBox;
    @InjectView(R.id.connection_auto_switch)
    CheckBox mConnectionAutoCheckBox;
    @InjectView(R.id.poweron_receiver_msg_switch)
    CheckBox mPoweronReceiverMsgCheckBox;
    @InjectView(R.id.send_crash_switch)
    CheckBox mSendCrashCheckBox;
    @InjectView(R.id.set_feedback)
    View mFeedBackView;
    @InjectView(R.id.set_about_textview)
    TextView mAboutView;
    @InjectView(R.id.exit_app)
    Button mExitBtn;


    private void checkVersion(ServerAppInfo serverInfo) {
        if (serverInfo.verCode > App.getAppVersionCode()) {
            notificateUpdateVersion(serverInfo);
        } else {
            Toast.makeText(getActivity(), getActivity().getString(R.string.update_no_new_version), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.set_about)
    public void doVersion() {
        VersionCheckTask mVersionCheckTask = new VersionCheckTask();
        mVersionCheckTask.setListener(serverInfoCheckTaskListener);
        mVersionCheckTask.execute();

    }

    private void notificateUpdateVersion(final ServerAppInfo serverInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(serverInfo.getApkUrl()));
                startActivity(browserIntent);
            }
        };
        builder.setTitle(getString(R.string.version_upgrade))
                .setMessage(getString(R.string.app_update) + serverInfo.verCode)
                .setPositiveButton(R.string.ok, positiveListener)
                .setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_settings, container,
                false);
        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        readData();
    }

    public void readData() {
        String me = App.mSmack.getUser();
        XMPPUtils.setImage(mHeadIcon, XMPPUtils.getJabberID(me));

        mStatusIcon.setImageResource(MainActivity.mStatusMap
                .get(PrefUtils.getPrefString(
                        PrefUtils.STATUS_MODE,
                        PrefUtils.AVAILABLE)));
        mStatusView.setText(PrefUtils.getPrefString(
                PrefUtils.STATUS_MESSAGE,
                getActivity().getString(R.string.status_available)));
        String account = App.readUser().getAccount();
        mNickView
                .setText(XMPPUtils.splitJidAndServer(account));
        mShowOfflineRosterCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.SHOW_OFFLINE, true));

        mNotifyRunBackgroundCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.FOREGROUND, true));
        mNewMsgSoundCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.SCLIENTNOTIFY, false));
        mNewMsgVibratorCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.VIBRATIONNOTIFY, true));
        mNewMsgLedCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.LEDNOTIFY, true));
        mVisiableNewMsgCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.TICKER, true));
        mShowHeadCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.SHOW_MY_HEAD, true));
        mConnectionAutoCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.AUTO_RECONNECT, true));
        mPoweronReceiverMsgCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.AUTO_START, true));
        mSendCrashCheckBox.setChecked(PrefUtils.getPrefBoolean(
                PrefUtils.REPORT_CRASH, true));

        mAboutView.setText("" + App.getAppVersionCode());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    }

    @Override
    public void onClick(View v) {
    }

    public void logoutDialog() {
    }
}
