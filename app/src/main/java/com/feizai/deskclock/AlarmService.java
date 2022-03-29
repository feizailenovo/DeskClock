package com.feizai.deskclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;

import com.feizai.deskclock.data.Alarm;
import com.feizai.deskclock.data.Alarms;
import com.feizai.deskclock.util.LogUtil;
import com.feizai.deskclock.util.MediaPlayerUtil;

/**
 * Author: chenhao
 * Date: 2022年3月26日 0026 下午 05:51:14
 * Describe: Manages alarms and vibrate. Runs as a service so that it can continue to play if another activity overrides the AlarmAlert dialog.
 * 管理闹钟和振动。 作为服务运行，以便在另一个活动覆盖 AlarmAlert 对话框时可以继续播放。
 */
public class AlarmService extends Service {

    private static final String PACKAGE_NAME = "com.feizai.deskclock";

    /**
     * Play alarm up to 10 minutes before silencing
     * 在静音前 10 分钟播放警报
     */
    private static final int ALARM_TIMEOUT_SECONDS = 2 * 60;

    private static final long[] sVibratePattern = new long[]{500, 500};

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;

    private boolean mPlaying = false;
    private Vibrator mVibrator;
    private MediaPlayerUtil mMediaPlayerUtil;
    private Alarm mCurrentAlarm;
    private long mStartTime;
    private TelephonyManager mTelephonyManager;
    private int mInitialCallState;
    private AudioManager mAudioManager = null;
    private boolean mCurrentStates = true;

    // Internal messages
    private static final int KILLER = 1;
    private static final int FOCUSCHANGE = 2;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILLER:
                    LogUtil.v("*********** Alarm killer triggered ***********");
                    sendKillBroadcast((Alarm) msg.obj);
                    stopSelf();
                    break;
                case FOCUSCHANGE:
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if (!mPlaying && mMediaPlayerUtil != null) {
                                stop();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (!mPlaying && mMediaPlayerUtil != null) {
                                mMediaPlayerUtil.pause();
                                mCurrentStates = false;
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (mPlaying && !mCurrentStates) {
                                play(mCurrentAlarm);
                            }
                            break;
                        default:
                            break;
                    }
                default:
                    break;
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {

            /**
             * The user might already be in a call when the alarm fires. When
             * we register onCallStateChanged, we get the initial in-call state
             * which kills the alarm. Check against the initial call state so
             * we don't kill the alarm during a call.
             * 警报触发时，用户可能已经在通话中。 当我们注册 onCallStateChanged 时，
             * 我们会获得终止警报的初始通话状态。 检查初始呼叫状态，这样我们就不会在呼叫期间终止警报。
             */
            if (state != TelephonyManager.CALL_STATE_IDLE
                    && state != mInitialCallState) {
                sendKillBroadcast(mCurrentAlarm);
                stopSelf();
            }
        }
    };

    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            mHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };

    @Override
    public void onCreate() {
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        /**
         * Listen for incoming calls to kill the alarm.
         * 收听来电以消除警报。
         */
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        AlarmAlertWakeLock.acquireCpuWakeLock(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /**
         * No intent, tell the system not to restart us.
         * 无intent，告诉系统不要重启我们。
         */
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final Alarm alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);

        if (alarm == null) {
            LogUtil.v("AlarmService failed to parse the alarm from the intent");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (mCurrentAlarm != null) {
            LogUtil.v("已经有一个闹钟响铃了");
            sendKillBroadcast(mCurrentAlarm);
        }

        play(alarm);
        mCurrentAlarm = alarm;
        /**
         * Record the initial call state here so that the new alarm has the newest state.
         * 在此处记录初始呼叫状态，以便新警报具有最新状态。
         */
        mInitialCallState = mTelephonyManager.getCallState();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stop();
        /**
         * Stop listening for incoming calls.
         * 停止收听来电。
         */
        mTelephonyManager.listen(mPhoneStateListener, 0);
        AlarmAlertWakeLock.releaseCpuLock();
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }

    private void sendKillBroadcast(Alarm alarm) {
        long millis = System.currentTimeMillis() - mStartTime;
        int minutes = (int) Math.round(millis / 60000.0);
        Intent alarmKilled = new Intent();
        alarmKilled.setAction(Alarms.ALARM_KILLED);
        alarmKilled.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        alarmKilled.putExtra(Alarms.ALARM_KILLED_TIMEOUT, minutes);
        alarmKilled.setPackage(PACKAGE_NAME);
        sendBroadcast(alarmKilled);
    }

    private void play(Alarm alarm) {
        /**
         * stop() checks to see if we are already playing.
         * stop() 检查我们是否已经在播放。
         */
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        stop();

        LogUtil.v("AlarmService.play() " + alarm.id + " alert " + alarm.alert);

        if (!alarm.silent) {
            Uri alert = alarm.alert;
            /**
             * Fall back on the default alarm if the database does not have an alarm stored.
             * 如果数据库没有存储闹铃，则使用默认闹铃。
             */
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                LogUtil.v("Using default alarm: " + alert.toString());
            }

            if (mMediaPlayerUtil == null) {
                mMediaPlayerUtil = MediaPlayerUtil.getInstance(this);
            }
            mMediaPlayerUtil.setMediaPlayerListener(new MediaPlayerUtil.MediaPlayerListener() {
                @Override
                public void onCompletion() {

                }

                @Override
                public void onError() {
                    LogUtil.e("Error occurred while playing audio.");
                    mMediaPlayerUtil.stop();
                    mMediaPlayerUtil.release();
                    mMediaPlayerUtil = null;
                }
            });

            try {
                /**
                 * Check if we are in a call. If we are, use the in-call alarm resource at a low volume to not disrupt the call.
                 * 检查我们是否在通话中。 如果是，请以低音量使用通话中警报资源，以免中断通话。
                 */
                if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                    LogUtil.v("Using the in-call alarm");
                    mMediaPlayerUtil.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                    mMediaPlayerUtil.setDataSource("in_call_alarm.ogg",true);
                } else {
                    mMediaPlayerUtil.setDataSource(alert,true);
                }
                startAlarm();
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.v("Using the fallback ringtone");
                /**
                 * The alert may be on the sd card which could be busy right now. Use the fallback ringtone.
                 * 铃声可能在 SD 卡上，现在可能很忙。 使用后备铃声。
                 */
                try {
                    mMediaPlayerUtil.setDataSource("fallbackring.ogg",true);
                    startAlarm();
                } catch (Exception e1) {
                    e.printStackTrace();
                    LogUtil.v("Failed to play fallback ringtone" + e);
                }
            }

            /**
             * Start the vibrator after everything is ok with the media player
             * 媒体播放器一切正常后启动振动器
             */
            if (alarm.vibrate) {
                mVibrator.vibrate(sVibratePattern, 0);
            } else {
                mVibrator.cancel();
            }

            enableKiller(alarm);
            mPlaying = true;
            mStartTime = System.currentTimeMillis();
        }
    }

    /**
     * Do the common stuff when starting the alarm.
     * 启动警报时做常见的事情。
     * @throws java.io.IOException
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    private void startAlarm() {
        final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0
        // (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            if (mMediaPlayerUtil != null) {
                mMediaPlayerUtil.setAudioStreamType(AudioManager.STREAM_ALARM);
//                mMediaPlayerUtil.setLoop(true);
                mMediaPlayerUtil.play();
            } else {
                LogUtil.v("MediaPlayerUtil is null");
            }
        }
    }

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not repeating
     * 如果不睡眠且不重复，则停止闹铃并禁用闹钟
     */
    public void stop() {
        LogUtil.v("AlarmService.stop()");
        if (mPlaying) {
            mPlaying = false;

            Intent alarmDone = new Intent();
            alarmDone.setAction(Alarms.ALARM_DONE_ACTION);
            alarmDone.setPackage(PACKAGE_NAME);
            sendBroadcast(alarmDone);

            /**
             * Stop vibrator
             * 停止振动器
             */
            mVibrator.cancel();
        }
        /**
         * Stop audio playing
         * 停止闹铃播放
         */
        if (mMediaPlayerUtil != null) {
            mMediaPlayerUtil.stop();
            mMediaPlayerUtil.release();
            mMediaPlayerUtil = null;
        }
        disableKiller();
    }

    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     * <p>
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     * <p>
     * 在 ALARM TIMEOUT_SECONDS 之后终止警报音频，因此警报不会整天运行。
     * 这只会取消音频，但会弹出通知，因此用户会知道警报已触发。
     */
    private void enableKiller(Alarm alarm) {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER, alarm),
                1000 * ALARM_TIMEOUT_SECONDS);
    }

    private void disableKiller() {
        mHandler.removeMessages(KILLER);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
