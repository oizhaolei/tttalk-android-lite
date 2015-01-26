package com.ruptech.tttalk_android.fragment;

import android.app.Dialog;
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

import com.ruptech.tttalk_android.R;
import com.ruptech.tttalk_android.activity.MainActivity;
import com.ruptech.tttalk_android.utils.XMPPHelper;
import com.ruptech.tttalk_android.utils.PrefUtils;

public class SettingsFragment extends Fragment implements OnClickListener,
        OnCheckedChangeListener {
    private View mAccountSettingView;
    private ImageView mHeadIcon;
    private ImageView mStatusIcon;
    private TextView mStatusView;
    private TextView mNickView;
    private CheckBox mShowOfflineRosterCheckBox;
    private CheckBox mNotifyRunBackgroundCheckBox;
    private CheckBox mNewMsgSoundCheckBox;
    private CheckBox mNewMsgVibratorCheckBox;
    private CheckBox mNewMsgLedCheckBox;
    private CheckBox mVisiableNewMsgCheckBox;
    private CheckBox mShowHeadCheckBox;
    private CheckBox mConnectionAutoCheckBox;
    private CheckBox mPoweronReceiverMsgCheckBox;
    private CheckBox mSendCrashCheckBox;
    private View mFeedBackView;
    private View mAboutView;
    private Button mExitBtn;
    private View mExitMenuView;
    private Button mExitCancleBtn;
    private Button mExitConfirmBtn;
    private Dialog mExitDialog;

    public static Fragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_settings_fragment, container,
                false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mExitMenuView = LayoutInflater.from(getActivity()).inflate(
                R.layout.common_menu_dialog_2btn_layout, null);
        mExitCancleBtn = (Button) mExitMenuView.findViewById(R.id.btnCancel);
        mExitConfirmBtn = (Button) mExitMenuView
                .findViewById(R.id.btn_exit_comfirm);
        mExitConfirmBtn.setText(R.string.exit);
        mExitCancleBtn.setOnClickListener(this);
        mExitConfirmBtn.setOnClickListener(this);
        mAccountSettingView = view.findViewById(R.id.accountSetting);
        mAccountSettingView.setOnClickListener(this);
        mHeadIcon = (ImageView) view.findViewById(R.id.face);
        mStatusIcon = (ImageView) view.findViewById(R.id.statusIcon);
        mStatusView = (TextView) view.findViewById(R.id.status);
        mNickView = (TextView) view.findViewById(R.id.nick);
        mShowOfflineRosterCheckBox = (CheckBox) view
                .findViewById(R.id.show_offline_roster_switch);
        mShowOfflineRosterCheckBox.setOnCheckedChangeListener(this);
        mNotifyRunBackgroundCheckBox = (CheckBox) view
                .findViewById(R.id.notify_run_background_switch);
        mNotifyRunBackgroundCheckBox.setOnCheckedChangeListener(this);
        mNewMsgSoundCheckBox = (CheckBox) view
                .findViewById(R.id.new_msg_sound_switch);
        mNewMsgSoundCheckBox.setOnCheckedChangeListener(this);
        mNewMsgVibratorCheckBox = (CheckBox) view
                .findViewById(R.id.new_msg_vibrator_switch);
        mNewMsgSoundCheckBox.setOnCheckedChangeListener(this);
        mNewMsgLedCheckBox = (CheckBox) view.findViewById(R.id.new_msg_led_switch);
        mNewMsgLedCheckBox.setOnCheckedChangeListener(this);
        mVisiableNewMsgCheckBox = (CheckBox) view
                .findViewById(R.id.visiable_new_msg_switch);
        mVisiableNewMsgCheckBox.setOnCheckedChangeListener(this);
        mShowHeadCheckBox = (CheckBox) view.findViewById(R.id.show_head_switch);
        mShowHeadCheckBox.setOnCheckedChangeListener(this);
        mConnectionAutoCheckBox = (CheckBox) view
                .findViewById(R.id.connection_auto_switch);
        mConnectionAutoCheckBox.setOnCheckedChangeListener(this);
        mPoweronReceiverMsgCheckBox = (CheckBox) view
                .findViewById(R.id.poweron_receiver_msg_switch);
        mPoweronReceiverMsgCheckBox.setOnCheckedChangeListener(this);
        mSendCrashCheckBox = (CheckBox) view.findViewById(R.id.send_crash_switch);
        mSendCrashCheckBox.setOnCheckedChangeListener(this);
        mFeedBackView = view.findViewById(R.id.set_feedback);
        mAboutView = view.findViewById(R.id.set_about);
        mExitBtn = (Button) view.findViewById(R.id.exit_app);
        mFeedBackView.setOnClickListener(this);
        mAboutView.setOnClickListener(this);
        mExitBtn.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        readData();
    }

    public void readData() {
        mHeadIcon.setImageResource(R.drawable.login_default_avatar);
        mStatusIcon.setImageResource(MainActivity.mStatusMap
                .get(PrefUtils.getPrefString(
                        PrefUtils.STATUS_MODE,
                        PrefUtils.AVAILABLE)));
        mStatusView.setText(PrefUtils.getPrefString(
                PrefUtils.STATUS_MESSAGE,
                getActivity().getString(R.string.status_available)));
        mNickView
                .setText(XMPPHelper.splitJidAndServer(PrefUtils
                        .getPrefString(
                                PrefUtils.ACCOUNT, "")));
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
