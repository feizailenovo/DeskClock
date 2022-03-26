package com.feizai.deskclock.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.IOException;

/**
 * Author: chenhao
 * Date: 2022年3月26日 0026 下午 08:06:12
 * Describe:
 */
public class MediaPlayerUtil {
    private static Context mContext;
    private MediaPlayer mMediaPlayer;
    private MediaPlayerListener mMediaPlayerListener;
    private String mFileName;
    private Uri mUri;
    private volatile Boolean isPrepared;
    private Boolean isLoop;

    private static MediaPlayerUtil instance;

    private MediaPlayerUtil() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            isPrepared = false;
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                LogUtil.e("Audio playback error." + what);
                isPrepared = false;
                if (mMediaPlayerListener != null) {
                    mMediaPlayerListener.onError();
                }
                return true;
            });
            mMediaPlayer.setOnPreparedListener(mp -> {
                LogUtil.i("Audio is ready.");
                isPrepared = true;
                if (mMediaPlayerListener != null) {
                    mMediaPlayerListener.onPrepared();
                }
            });
            mMediaPlayer.setOnCompletionListener(mp -> {
                LogUtil.i("Audio has finished playing.");
                isPrepared = false;
                if (mMediaPlayerListener != null) {
                    mMediaPlayerListener.onCompletion();
                }
            });
        }
    }

    public static synchronized MediaPlayerUtil getInstance(@NonNull Context context) {
        mContext = context;
        if (instance == null) {
            instance = new MediaPlayerUtil();
        }
        return instance;
//        return MediaPlayerUtilHolder.sInstance;
    }

//    private static class MediaPlayerUtilHolder {
//        private static final MediaPlayerUtil sInstance = new MediaPlayerUtil();
//    }

    public MediaPlayerUtil setDataSource(String fileName, boolean isLoop) {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.reset();
                if (TextUtils.isEmpty(fileName)) {
                    LogUtil.e("Datasource is null");
                }else {
                    mFileName = fileName;
                    if (fileName.contains("/")) {
                        mMediaPlayer.setDataSource(fileName);
                    } else {
                        AssetFileDescriptor assetFileDescriptor = mContext.getResources().getAssets().openFd(fileName);
                        mMediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
                    }
                    if (isLoop) {
                        mMediaPlayer.setLooping(true);
                    }
                    this.isLoop = isLoop;
                    mMediaPlayer.prepare();
                }
            } catch (IOException e) {
                LogUtil.e("Resource loading failed.");
                LogUtil.e(e.getMessage());
                e.printStackTrace();
            }
        }
        return this;
    }

    public MediaPlayerUtil setDataSource(@NonNull Uri uri, boolean isLoop) {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.reset();
                mFileName = "";
                mUri = uri;
                mMediaPlayer.setDataSource(mContext,uri);
                if (isLoop) {
                    mMediaPlayer.setLooping(true);
                }
                this.isLoop = isLoop;
                mMediaPlayer.prepare();
            } catch (IOException e) {
                LogUtil.e("Resource loading failed.");
                LogUtil.e(e.getMessage());
                e.printStackTrace();
            }
        }
        return this;
    }

    public MediaPlayerUtil play() {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            if (!isPrepared){
                if (!TextUtils.isEmpty(mFileName)) {
                    setDataSource(mFileName, isLoop);
                } else if (mUri != null) {
                    setDataSource(mUri, isLoop);
                }
            }
            mMediaPlayer.start();
        }
        return this;
    }

    public MediaPlayerUtil stop() {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                isPrepared = false;
            }
        }
        return this;
    }

    public MediaPlayerUtil pause() {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        }
        return this;
    }

    public void setLoop(Boolean value) {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            mMediaPlayer.setLooping(value);
            isLoop = value;
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            mMediaPlayer.setVolume(leftVolume, rightVolume);
        }
    }

    public void setAudioStreamType(int streamtype) {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            mMediaPlayer.setAudioStreamType(streamtype);
        }
    }

    public void release() {
        if (mMediaPlayer == null) {
            LogUtil.i("MediaPlayer is null");
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        instance = null;
        LogUtil.i("MediaPlayer release complete.");
    }

    public MediaPlayerUtil setMediaPlayerListener(MediaPlayerListener mediaPlayerListener) {
        if (mediaPlayerListener == null) {
            throw new NullPointerException("MediaPlayerListener is null");
        }
        mMediaPlayerListener = mediaPlayerListener;
        return this;
    }

    public interface MediaPlayerListener {
        void onCompletion();

        default void onPrepared() {
        }

        void onError();
    }

}
