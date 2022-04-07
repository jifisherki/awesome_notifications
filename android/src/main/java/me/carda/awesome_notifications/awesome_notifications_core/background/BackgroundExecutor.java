package me.carda.awesome_notifications.awesome_notifications_core.background;

import android.content.Context;
import android.content.Intent;

import me.carda.awesome_notifications.awesome_notifications_core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.awesome_notifications_core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.awesome_notifications_core.exceptions.ExceptionFactory;

public abstract class BackgroundExecutor {

    private static final String TAG = "BackgroundExecutor";

    private static BackgroundExecutor runningInstance;

    protected Long dartCallbackHandle = 0L;
    protected Long silentCallbackHandle = 0L;

    private static Class<? extends BackgroundExecutor> awesomeBackgroundExecutorClass;

    public static void setBackgroundExecutorClass (
            Class<? extends BackgroundExecutor> awesomeBackgroundExecutorClass
    ){
        BackgroundExecutor.awesomeBackgroundExecutorClass =
                awesomeBackgroundExecutorClass;
    }

    public abstract boolean isDone();
    public abstract boolean runBackgroundAction(Context context, Intent silentIntent);

    public static void runBackgroundExecutor(
        Context context,
        Intent silentIntent,
        Long dartCallbackHandle,
        Long silentCallbackHandle
    ) throws AwesomeNotificationsException {

        try {

            if(awesomeBackgroundExecutorClass == null)
                throw ExceptionFactory
                        .getInstance()
                        .createNewAwesomeException(
                                TAG,
                                ExceptionCode.INITIALIZATION_EXCEPTION,
                                "There is no valid background executor available to run.");

            if(runningInstance == null || runningInstance.isDone()) {

                runningInstance =
                        awesomeBackgroundExecutorClass.newInstance();

                runningInstance.dartCallbackHandle = dartCallbackHandle;
                runningInstance.silentCallbackHandle = silentCallbackHandle;
            }

            if(!runningInstance.runBackgroundAction(
                    context,
                    silentIntent
            )){
                runningInstance = null;
                throw ExceptionFactory
                        .getInstance()
                        .createNewAwesomeException(
                                TAG,
                                ExceptionCode.BACKGROUND_EXECUTION_EXCEPTION,
                                "The background executor could not be started.");
            }

        } catch (IllegalAccessException | InstantiationException e) {
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.BACKGROUND_EXECUTION_EXCEPTION,
                            String.format("%s", e.getLocalizedMessage()),
                            e);
        }
    }
}