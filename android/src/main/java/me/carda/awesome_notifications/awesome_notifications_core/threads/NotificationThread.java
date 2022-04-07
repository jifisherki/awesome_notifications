package me.carda.awesome_notifications.awesome_notifications_core.threads;

import android.os.*;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.carda.awesome_notifications.awesome_notifications_core.enumerators.MediaSource;
import me.carda.awesome_notifications.awesome_notifications_core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.awesome_notifications_core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.awesome_notifications_core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.awesome_notifications_core.models.NotificationModel;
import me.carda.awesome_notifications.awesome_notifications_core.utils.BitmapUtils;

public abstract class NotificationThread<T>{

    private final String TAG = "NotificationThread";

    protected abstract T doInBackground() throws Exception;
    protected abstract T onPostExecute(@Nullable T received) throws AwesomeNotificationsException;
    protected abstract void whenComplete(@Nullable T returnedValue, @Nullable AwesomeNotificationsException exception);

    public void execute(){
        runOnBackgroundThread();
    }

    public void execute(NotificationModel notificationModel){
        if(itMustRunOnBackgroundThread(notificationModel))
            runOnBackgroundThread();
        else
            runOnForegroundThread();
    }

    private void runOnBackgroundThread() {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());
        final NotificationThread<T> threadReference = this;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    final T response = threadReference.doInBackground();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            T returnedValue = null;
                            try{
                                returnedValue = threadReference.onPostExecute(response);
                                whenComplete(returnedValue, null);
                            } catch (AwesomeNotificationsException awesomeException) {
                                whenComplete(null, awesomeException);
                            } catch (Exception exception){
                                whenComplete(
                                        null,
                                        ExceptionFactory
                                                .getInstance()
                                                .createNewAwesomeException(
                                                        TAG,
                                                        ExceptionCode.NOTIFICATION_THREAD_EXCEPTION,
                                                        exception));
                            }
                        }
                    });
                } catch (AwesomeNotificationsException awesomeException) {
                    whenComplete(null, awesomeException);
                } catch (Exception exception) {
                    whenComplete(
                            null,
                            ExceptionFactory
                                    .getInstance()
                                    .createNewAwesomeException(
                                            TAG,
                                            ExceptionCode.NOTIFICATION_THREAD_EXCEPTION,
                                            exception));
                }
            }
        });
    }

    private void runOnForegroundThread() {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            T returnedValue = null;
            try{
                returnedValue = onPostExecute(doInBackground());
                whenComplete(returnedValue, null);
            } catch (AwesomeNotificationsException awesomeException) {
                whenComplete(null, awesomeException);
            } catch (Exception exception){
                whenComplete(
                        null,
                        ExceptionFactory
                                .getInstance()
                                .createNewAwesomeException(
                                        TAG,
                                        ExceptionCode.NOTIFICATION_THREAD_EXCEPTION,
                                        exception));
            }
        }
        else {
            final Handler handler = new Handler(Looper.getMainLooper());
            final NotificationThread<T> threadReference = this;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    T returnedValue = null;
                    try {
                        final T response = threadReference.doInBackground();
                        returnedValue = threadReference.onPostExecute(response);
                        whenComplete(returnedValue, null);
                    } catch (AwesomeNotificationsException awesomeException) {
                        whenComplete(null, awesomeException);
                    } catch (Exception exception){
                        whenComplete(
                                null,
                                ExceptionFactory
                                        .getInstance()
                                        .createNewAwesomeException(
                                                TAG,
                                                ExceptionCode.NOTIFICATION_THREAD_EXCEPTION,
                                                exception));
                    }
                }
            });
        }
    }

    private boolean itMustRunOnBackgroundThread(NotificationModel notificationModel){
        BitmapUtils bitmapUtils = BitmapUtils.getInstance();
        return
                MediaSource.Network == bitmapUtils
                        .getMediaSourceType(notificationModel.content.bigPicture)
                ||
                MediaSource.Network == bitmapUtils
                        .getMediaSourceType(notificationModel.content.largeIcon);
    }

}