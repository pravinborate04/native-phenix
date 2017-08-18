package com.nativephenix.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.nativephenix.App;
import com.nativephenix.AppPhenixData;
import com.nativephenix.Capabilities;
import com.nativephenix.R;
import com.nativephenix.ServerAddress;
import com.nativephenix.presenter.MainPresenter;
import com.nativephenix.presenter.inter.IMainPresenter;
import com.nativephenix.ui.view.IMainActivityView;
import com.nativephenix.utils.DialogUtil;
import com.nativephenix.utils.TokenUtil;
import com.nativephenix.utils.Utilities;
import com.phenixp2p.environment.android.AndroidContext;
import com.phenixp2p.pcast.DataQualityReason;
import com.phenixp2p.pcast.DataQualityStatus;
import com.phenixp2p.pcast.MediaStream;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.PCastFactory;
import com.phenixp2p.pcast.PCastInitializeOptions;
import com.phenixp2p.pcast.Publisher;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RendererStartStatus;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.UserMediaOptions;
import com.phenixp2p.pcast.UserMediaStream;
import com.phenixp2p.pcast.android.AndroidPCastFactory;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.nativephenix.Constants.APP_TAG;
import static com.nativephenix.Constants.CREATE_SCREEN_CAPTURE;
import static com.nativephenix.Constants.NUMBER_TOUCHES;
import static com.nativephenix.Constants.NUM_HTTP_RETRIES;
import static com.nativephenix.Constants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS;
import static com.nativephenix.Constants.REQUEST_CODE_SECRET_URL;
import static com.nativephenix.Constants.SESSION_ID;
import static com.nativephenix.Constants.STREAM_ID;
import static com.nativephenix.Constants.TIME_TO_TAP;
import static com.nativephenix.utils.Utilities.handleException;
import static com.nativephenix.utils.Utilities.hasInternet;

public class MainActivity extends AppCompatActivity implements IMainActivityView,Publisher.DataQualityChangedCallback{


    private IMainPresenter presenter;
    private String sessionId;
    private static final int ACTION_WIFI = 120;
    private SurfaceView localSurfaceView;
    private SurfaceHolder localSurfaceHolder;
    private PCast pcast;
    private boolean isVideo = false;
    private boolean isOnlyVideoOrAudio = false;
    private final UserMediaOptions gumOptions = new UserMediaOptions();
    private UserMediaStream publishMedia;
    private Handler mainHandler;
    private Renderer renderPreView;
    private String streamId;
    // private PulsatorLayout pulsator;
    private Publisher publisher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mainHandler = new Handler(Looper.getMainLooper());

        localSurfaceView=(SurfaceView)findViewById(R.id.localSurfaceView);
        localSurfaceHolder = localSurfaceView.getHolder();

        presenter=new MainPresenter(this);

       // checkPermissions();
        this.presenter.login("demo-user", "demo-password", ServerAddress.PRODUCTION_ENDPOINT.getServerAddress());
    }

    @Override
    public void authenticationToken(String authenticationToken) {
        Toast.makeText(this, authenticationToken, Toast.LENGTH_SHORT).show();
        start(authenticationToken);

    }

    private void start(final String authenticationToken) {
        String pcastUrl = AppPhenixData.pcastAddress;
        Log.d(APP_TAG, "2. PCast SDK API: start [" + (pcastUrl == null ? "" : pcastUrl) + "]");
        if (pcastUrl == null) {
            this.pcast = AndroidPCastFactory.createPCast(this);
        } else {
            AndroidContext.setContext(this);
            this.pcast = PCastFactory.createPCast(pcastUrl);
        }
        this.pcast.initialize(new PCastInitializeOptions(false, true));
        this.pcast.start(authenticationToken, new PCast.AuthenticationCallback() {
                    public void onEvent(PCast var1, final RequestStatus status, final String sessionId) {
                        MainActivity.this.mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == RequestStatus.OK) {
                                    AppPhenixData.pCast=MainActivity.this.pcast;
                                    MainActivity.this.sessionId = sessionId;
                                    if (MainActivity.this.isOnlyVideoOrAudio) {
                                        MainActivity.this.onlyVideoOrAudio(MainActivity.this.isVideo);
                                    } else {
                                        MainActivity.this.getDefaultUserMedia();
                                    }
                                } else {
                                    Log.e("Error","render error");
                                    MainActivity.this.onTryAfterError(getResources().getString(R.string.render_error, status.name()));
                                }
                            }
                        });
                    }
                },
                new PCast.OnlineCallback() {
                    public void onEvent(PCast var1) {
                        Log.d(APP_TAG, "SDK online");
                    }
                },
                new PCast.OfflineCallback() {
                    public void onEvent(PCast var1) {
                        Log.d(APP_TAG, "SDK offline");
                    }
                });
    }

    public void onlyVideoOrAudio(boolean isVideo) {
        this.isOnlyVideoOrAudio = true;
        if (isVideo) {
            this.isVideo = true;
            this.gumOptions.getAudioOptions().setEnabled(false);
            this.gumOptions.getVideoOptions().setEnabled(true);
            this.gumOptions.getVideoOptions().setDeviceId(null);
        } else {
            this.isVideo = false;
            this.gumOptions.getAudioOptions().setEnabled(true);
            this.gumOptions.getAudioOptions().setDeviceId(null);
            this.gumOptions.getVideoOptions().setEnabled(false);
        }
        this.getUserMedia();
    }

    private void getDefaultUserMedia() {
        this.gumOptions.getAudioOptions().setEnabled(true);
        this.gumOptions.getAudioOptions().setDeviceId(null);
        this.gumOptions.getVideoOptions().setEnabled(true);
        this.gumOptions.getVideoOptions().setDeviceId(null);
        this.getUserMedia();
    }

    private void getUserMedia() {
        Log.d(APP_TAG, "3. Get user publishMedia from SDK");
        try {
            if (this.pcast != null) {
                this.pcast.getUserMedia(this.gumOptions, new PCast.UserMediaCallback() {
                    public void onEvent(PCast p, final RequestStatus status, final UserMediaStream media) {
                        MainActivity.this.mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == RequestStatus.OK) {
                                    if (media != null) {
                                        MainActivity.this.publishMedia = media;
                                        MainActivity.this.getPublishToken();
                                    } else {
                                        MainActivity.this.onTryAfterError(getResources().getString(R.string.media_null));
                                    }
                                } else {
                                    MainActivity.this.onTryAfterError(getResources().getString(R.string.render_error, status.name()));
                                }
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            handleException(this, e);
        }
    }

    private void onTryAfterError(final String title) {
        if (AppPhenixData.isShare) {
            /*if (PhenixService.isReady()) {
                this.stopService(new Intent(this, PhenixService.class));
            }*/
        }
       // this.onClear();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogUtil.showDialog(title, "Please try again", new DialogUtil.ActionDialog() {
                    @Override
                    public AppCompatActivity getContext() {
                        return MainActivity.this;
                    }

                    @Override
                    public void buttonYes() {
                        MainActivity.this.login();
                    }

                    @Override
                    public void autoDismiss(AlertDialog alertDialog) {}
                });
            }
        });
    }

    private String getSessionId() {
        return this.sessionId == null ? TokenUtil.getSessionIdLocal(this) : this.sessionId;
    }

    // 4. Get publish token from REST admin API.
    private void getPublishToken() {
        Log.d(APP_TAG, "4. Get publish token from REST admin API");
        Log.e("StmToken getSrvrAddress",ServerAddress.PRODUCTION_ENDPOINT.getServerAddress());
        Log.e("sessionId",this.getSessionId());
        presenter.createStreamToken(ServerAddress.PRODUCTION_ENDPOINT.getServerAddress(),
                this.getSessionId(),
                null,
                new String[]{Capabilities.STREAMING.getValue()},
                new MainPresenter.IStreamer() {
                    @Override
                    public void hereIsYourStreamToken(String streamToken) {
                        if (streamToken != null) {
                            MainActivity.this.publishStream(streamToken);
                        } else {
                            MainActivity.this.getPublishToken();
                        }
                    }

                    @Override
                    public void isError(int count) {
                        if (count == NUM_HTTP_RETRIES) {
                            Log.w(APP_TAG, "Failed to obtain publish token after [" + count + "] retries");
                           // MainActivity.this.setGoneVersion();
                            // TODO(NL): This should be refactored to display the error in MainFragment instead
                            MainActivity.this.onTryAfterError("Failed to obtain publish token");
                        }
                    }
                });
    }

    // 5. Publish streamToken with SDK.
    private void publishStream(String publishStreamToken) {
        if (this.pcast != null && this.publishMedia != null ) {
            Log.d(APP_TAG, "5. Publish streamToken with SDK");
            MediaStream mediaStream = this.publishMedia.getMediaStream();
            if (mediaStream == null) {
                return;
            }
            this.pcast.publish(publishStreamToken, mediaStream, new PCast.PublishCallback() {
                public void onEvent(PCast p, final RequestStatus status, final Publisher publisher) {
                    MainActivity.this.mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (status == RequestStatus.OK) {
                                didPublishStream(publisher);
                                renderVideo();
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                       /* if (MainActivity.this.progressBar != null) {
                                            MainActivity.this.progressBar.setVisibility(View.GONE);
                                        }*/
                                        MainActivity.this.onTryAfterError(getResources().getString(R.string.render_error, status.name()));
                                    }
                                });
                            }
                        }
                    });
                }
            });
        } else {
            this.onTryAfterError("Media is null");
        }
    }

    private void didPublishStream(final Publisher publisher) {
        if (this.publisher != null && this.publisher.hasEnded() && !this.publisher.isClosed()) {
            this.publisher.stop("close");
            Utilities.close(this, this.publisher);
        }
        this.publisher = publisher;
        this.streamId = publisher.getStreamId();
        this.publisher.setDataQualityChangedCallback(this);


    }

    private void renderVideo() {
        this.renderPreView = publishMedia.getMediaStream().createRenderer();
        if (this.renderPreView.start(new AndroidVideoRenderSurface(this.localSurfaceHolder)) == RendererStartStatus.OK) {
            Log.e("renderVideo", "insde");
        }
    }


    @Override
    protected void onPause() {
        if (!AppPhenixData.isShare) {
            AppPhenixData.isStopPublish=false;
            AppPhenixData.isBackground=false;
            this.onClear();
            if (this.presenter != null) {
                this.presenter.onDestroy();
            }
            /*this.progressBar.setVisibility(View.GONE);
            if (this.subscriptions != null) {
                this.subscriptions.clear();
                this.subscriptions.unsubscribe();
                this.subscriptions = null;
            }*/
            this.mainHandler = null;
            System.gc();
        }


        super.onPause();
    }

    public void onClear() {
        // Stop pcast when we exit the app.
        if (this.publisher != null) {
            this.publisher.stop("exit-app");
            if (!this.publisher.isClosed()) {
                Utilities.close(this, this.publisher);
                this.publisher = null;
            }
        }

        if (this.publishMedia != null) {
            //not call close(), because when reopen will error
            this.publishMedia = null;
        }

        if (this.pcast != null) {
            this.pcast.stop();
            this.pcast.shutdown();
            if (!pcast.isClosed()) {
                Utilities.close(this, this.pcast);
            }
            AppPhenixData.pCast=null;
            this.pcast = null;
        }
    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void showProgress() {

    }

    @Override
    public void hideProgress() {

    }


    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.CAMERA)) {
            permissionsNeeded.add("access the camera");
        }

        if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO)) {
            permissionsNeeded.add("access the microphone");
        }

        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                !addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissionsNeeded.add("write and read access for logs");
        }

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++) {
                    message = message + ", " + permissionsNeeded.get(i);
                }
                showMessageOKCancel(message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                permissionsList.toArray(new String[permissionsList.size()]),
                                REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                    }
                });
                return;
            }
            ActivityCompat.requestPermissions(this,
                    permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }
        this.commenceSession();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> permissionCodes = new ArrayMap<>();
                // Initial
                permissionCodes.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_DENIED);
                permissionCodes.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_DENIED);
                permissionCodes.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_DENIED);
                permissionCodes.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_DENIED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++) {
                    permissionCodes.put(permissions[i], grantResults[i]);
                }
                // Check for CAMERA
                if (permissionCodes.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && permissionCodes.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        && permissionCodes.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        || permissionCodes.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    this.commenceSession();
                } else {
                    // Permission Denied
                    Toast.makeText(this, getResources().getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return false;
            }
        }
        return true;
    }


    private void commenceSession() {
        if (this.sessionId == null) {
            this.login();
        }
    }


    public void login() {
        // Check the connection to the internet.
        if (hasInternet(this)) {
            Log.d(APP_TAG, "1. REST API: authenticate");
            this.presenter.login("demo-user", "demo-password", ServerAddress.PRODUCTION_ENDPOINT.getServerAddress());
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(MainActivity.this).setTitle("No internet")
                            .setMessage("Please connect to the internet")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(Intent.ACTION_MAIN);
                                            intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
                                            startActivityForResult(intent, ACTION_WIFI);
                                            dialogInterface.dismiss();
                                        }
                                    }
                            ).show();
                }
            });
        }
    }

    @Override
    public void onEvent(Publisher publisher, DataQualityStatus dataQualityStatus, DataQualityReason dataQualityReason) {

    }
}
