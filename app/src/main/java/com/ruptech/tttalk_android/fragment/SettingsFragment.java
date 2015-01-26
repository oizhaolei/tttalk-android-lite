package com.ruptech.tttalk_android.fragment;

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

import com.ruptech.tttalk_android.App;
import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.activity.MainActivity;
import com.ruptech.tttalk_android.utils.PrefUtils;
import com.ruptech.tttalk_android.utils.XMPPUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SettingsFragment extends Fragment implements OnClickListener,
        OnCheckedChangeListener {
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
    @InjectView(R.id.set_about)
    View mAboutView;
    @InjectView(R.id.exit_app)
    Button mExitBtn;

    public static Fragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_settings_fragment, container,
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
        String account = PrefUtils
                .getPrefString(
                        PrefUtils.ACCOUNT, "");
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
