package com.nativephenix.presenter;

import android.util.Log;

import com.nativephenix.AsyncService;
import com.nativephenix.HttpTask;
import com.nativephenix.model.AuthenticationRequest;
import com.nativephenix.model.AuthenticationResponse;
import com.nativephenix.model.StreamTokenRequest;
import com.nativephenix.model.StreamTokenResponse;
import com.nativephenix.presenter.inter.IMainPresenter;
import com.nativephenix.ui.view.IMainActivityView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

import static com.nativephenix.Constants.APP_TAG;
import static com.nativephenix.HttpTask.Method.POST;

/**
 * Created by Pravin Borate on 16/8/17.
 */

public class MainPresenter implements IMainPresenter{

    private IMainActivityView activityView;


    public MainPresenter(IMainActivityView activityView) {
        this.activityView = activityView;
    }

    @Override
    public void login(String user, String password, String endpoint) {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setName(user);
        request.setPassword(password);
        HttpTask<AuthenticationRequest, AuthenticationResponse> task = new HttpTask<>(new HttpTask.Callback<AuthenticationResponse>() {
            @Override
            public void onResponse(AuthenticationResponse result) {
                if (result != null) {
                    MainPresenter.this.activityView.authenticationToken(result.getAuthenticationToken());
                }
            }

            @Override
            public void onError(Exception e) {
                MainPresenter.this.activityView.onError(e.getMessage());
            }
        }, endpoint.concat("login"), POST, request, AuthenticationResponse.class);
        task.execute(AsyncService.getInstance().getExecutorService());
    }

    @Override
    public void startRendering() {

    }

    @Override
    public void listStreams(int length, String endpoint) {

    }

    @Override
    public void createStreamToken(String endpoint, String sessionId, final String originStreamId, String[] capabilities, final IStreamer streamer) {
        Log.e("createStremToken","endPoint "+endpoint+"\nsessionID"+sessionId+"\noriginStreamId "+originStreamId+"\nCapabilities "+capabilities);
        StreamTokenRequest request = new StreamTokenRequest();
        request.setSessionId(sessionId);
        if (originStreamId != null) {
            request.setOriginStreamId(originStreamId);
        }

        if (capabilities != null) {
            List<String> listCapabilities = new ArrayList<>();
            Collections.addAll(listCapabilities, capabilities);
            request.setCapabilities(listCapabilities);
        }
        HttpTask<StreamTokenRequest, StreamTokenResponse> task = new HttpTask<>(new HttpTask.Callback<StreamTokenResponse>() {
            private int errorCount = 0;

            @Override
            public void onResponse(StreamTokenResponse result) {
                if (result != null) {
                    streamer.hereIsYourStreamToken(result.getStreamToken());
                    if (originStreamId != null && MainPresenter.this.activityView != null) {
                        MainPresenter.this.activityView.hideProgress();
                    }
                } else {
                    if (activityView != null) {
                        MainPresenter.this.activityView.hideProgress();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(APP_TAG, "Caught error [" + e + "] while attempting to obtain stream token");
                this.errorCount++;
                streamer.isError(this.errorCount);
            }
        }, endpoint.concat("stream"), POST, request, StreamTokenResponse.class);
        task.execute(AsyncService.getInstance().getExecutorService());
    }

    @Override
    public void onDestroy() {

    }


    public interface IStreamer {
        void hereIsYourStreamToken(String streamToken);
        void isError(int count);
    }
}
