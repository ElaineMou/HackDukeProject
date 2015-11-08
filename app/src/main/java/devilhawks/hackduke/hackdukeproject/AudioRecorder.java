package devilhawks.hackduke.hackdukeproject;

/**
 * Created by abhishekupadhyayaghimire on 11/7/15.
 * The application needs to have the permission to write to external storage
 * if the output file is written to the external storage, and also the
 * permission to record button. These permissions must be set in the
 * application's AndroidManifest.xml file, with something like:
 *
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 * <uses-permission android:name="android.permission.RECORD_AUDIO" />
 *
 */

    import android.annotation.TargetApi;
    import android.app.Activity;
    import android.content.Intent;
    import android.content.IntentFilter;
    import android.content.Context;
    import android.os.Bundle;
    import android.os.Environment;

    import android.view.View;
    import android.view.ViewGroup;
    import android.view.View.OnClickListener;

    import android.widget.Button;
    import android.widget.Toast;
    import android.widget.LinearLayout;

    import android.util.Log;

    import android.media.MediaRecorder;
    import android.media.MediaPlayer;

    import android.media.MediaCodec;
    import android.media.MediaFormat;

    import java.io.IOException;
    import java.nio.ByteBuffer;


public class AudioRecorder extends Activity {
    private static final String LOG_TAG = "AudioRecorder";
    private static String mFileName = null;

    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private PlayButton mPlayButton = null;
    private MediaPlayer mPlayer = null;

    private MediaCodec mMediaCodec;

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        Log.d(LOG_TAG, "started recording");

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    class RecordButton extends Button {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                Log.d(LOG_TAG, "got into the clicker");
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;

            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends Button {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }

    public AudioRecorder() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        LinearLayout ll = new LinearLayout(this);
        mRecordButton = new RecordButton(this);
        ll.addView(mRecordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        mPlayButton = new PlayButton(this);
        ll.addView(mPlayButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        setContentView(ll);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    /*
    @TargetApi(17)
    public void parse() {
        //MediaCodec mMediaCodec = MediaCodec.createByCodecName("aad");
        String mMime = "audio/3gp";
        //try {
        mMediaCodec = MediaCodec.createDecoderByType(mMime);
        //}catch(IOException e){
//                Log.d(LOG_TAG,"Caught IOException: " + e.getMessage());

        //          }
        MediaFormat mMediaFormat = new MediaFormat();
        mMediaFormat = MediaFormat.createAudioFormat(mMime,
                mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));

        mMediaCodec.configure(mMediaFormat, null, null, 0);
        mMediaCodec.start();
        try {
            MediaCodec.BufferInfo buf_info = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(buf_info, 0);
            byte[] pcm = new byte[buf_info.size];

            ByteBuffer[] outputBuf = new ByteBuffer[buf_info.size];
            outputBuf[outputBufferIndex].get(pcm, 0, buf_info.size);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }*/
}