package br.com.horusapp.ui;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.com.horusapp.R;
import br.com.horusapp.network.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static final String PARAM_USER_EMAIL = "param_user_email";
    public static final String PARAM_USER_TOKEN = "param_user_token";

    private FloatingActionButton fabRecord;
    private RelativeLayout layoutProgress;
    private EditText txtTitle;
    private EditText txtLocation;

    private Camera camera;
    private SurfaceHolder holder;
    private MediaRecorder recorder;
    private Handler handler;
    private Runnable recordRepeater;
    private boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        initFabClick();
        initSurfaceHolder();

        txtTitle = (EditText) findViewById(R.id.txt_title);
        txtLocation = (EditText) findViewById(R.id.txt_location);

        findViewById(R.id.img_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFilesToFirebase();
            }
        });

        hideLoading();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop Record Repeater
        if (handler != null)
            handler.removeCallbacks(recordRepeater);

        if (isRecording)
            stopRecording(true);
    }

    private void initFabClick() {
        fabRecord = (FloatingActionButton) findViewById(R.id.fab_record);
        fabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    stopRecording(true);
                } else {
                    startRecording(getNewFile());
                }
            }
        });
    }

    private void initSurfaceHolder() {
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        int cameraId = CameraHelper.findFrontFacingCameraID();
        camera = Camera.open(cameraId);

        try {
            CameraHelper.setCameraDisplayOrientation(CameraActivity.this, cameraId, camera);
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException exception) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    private void startRecording(File videoFile) {
        if (!isRecording) {
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA);
            profile.videoCodec = MediaRecorder.VideoEncoder.H264;

            camera.unlock();
            recorder = new MediaRecorder();
            recorder.setCamera(camera);
            recorder.setMaxDuration(1000 * 60);
            recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
            recorder.setProfile(profile);
            recorder.setOutputFile(videoFile.getPath());
            recorder.setPreviewDisplay(holder.getSurface());

            try {
                recorder.prepare();
                recorder.start();
                isRecording = true;

                // Restart recording every 60 seconds
                recordRepeater = new Runnable() {
                    @Override
                    public void run() {
                        stopRecording(false);
                        startRecording(getNewFile());
                    }
                };

                handler = new Handler();
                handler.postDelayed(recordRepeater, 1000 * 60);
            } catch (IllegalStateException | IOException e) {
                e.printStackTrace();
            }
        }

        fabRecord.setImageResource(R.drawable.ic_pause_white_24dp);
        txtTitle.setEnabled(false);
        txtLocation.setEnabled(false);
    }

    private void stopRecording(boolean stopRepeater) {
        if (isRecording) {
            recorder.stop();
            recorder.release();
            isRecording = false;
        }

        if (stopRepeater && handler != null)
            handler.removeCallbacks(recordRepeater);

        fabRecord.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        txtTitle.setEnabled(true);
        txtLocation.setEnabled(true);
    }

    private File getNewFile() {
        File folder = new File(Environment.getExternalStorageDirectory(), "HorusApp/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd__hh_mm_ss");
        File file = new File(folder, format.format(new Date()) + ".mp4");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    private void uploadFilesToFirebase() {
        showLoading();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReferenceFromUrl("gs://horusapp-d469a.appspot.com");

                    File folder = new File(Environment.getExternalStorageDirectory(), "HorusApp/");
                    for (final File videofile : folder.listFiles()) {
                        Uri file = Uri.fromFile(videofile);
                        StorageReference riversRef = storageRef.child(file.getLastPathSegment());
                        UploadTask uploadTask = riversRef.putFile(file);

                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                hideLoading();
                                Log.e("HorusApp", "ERROR Uploading " + videofile.getPath());
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                videofile.delete();

                                sendVideoToAPI(downloadUrl.toString());
                                Log.i("HorusApp", "UPLOADED " + downloadUrl.toString());
                            }
                        });
                    }
                } else {
                    hideLoading();
                }
            }
        });
    }

    private void sendVideoToAPI(String videoUrl) {
        ApiService apiService = ApiService.retrofit.create(ApiService.class);

        String userEmail = getIntent().getStringExtra(PARAM_USER_EMAIL);
        String userToken = getIntent().getStringExtra(PARAM_USER_TOKEN);

        Call<ResponseBody> call = apiService.sendVideo(userEmail, userToken, 1, videoUrl,
                txtTitle.getText().toString(),
                txtLocation.getText().toString());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Toast.makeText(CameraActivity.this, getString(R.string.video_uploaded), Toast.LENGTH_LONG).show();
                hideLoading();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CameraActivity.this, getString(R.string.error_uploading_video), Toast.LENGTH_LONG).show();
                hideLoading();
            }
        });
    }

    private void showLoading() {
        layoutProgress = (RelativeLayout) findViewById(R.id.layout_progress);
        layoutProgress.setVisibility(View.VISIBLE);

        Log.i("HorusApp", "showLoading: ");
    }

    private void hideLoading() {
        layoutProgress = (RelativeLayout) findViewById(R.id.layout_progress);
        layoutProgress.setVisibility(View.GONE);

        Log.i("HorusApp", "hideLoading: ");
    }
}
