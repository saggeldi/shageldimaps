package app.organicmaps;

import static app.organicmaps.sdk.location.LocationState.LOCATION_TAG;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.preference.PreferenceManager;

import com.tut.courier.ui.KoinInitializer;
import com.tut.courier.ui.TutAppFactory;
import com.tut.courier.ui.TutMapListener;

import org.jetbrains.annotations.NotNull;

import app.organicmaps.background.OsmUploadWork;
import app.organicmaps.downloader.DownloaderNotifier;
import app.organicmaps.location.LocationProviderFactoryImpl;
import app.organicmaps.location.TrackRecordingService;
import app.organicmaps.routing.NavigationService;
import app.organicmaps.sdk.Map;
import app.organicmaps.sdk.OrganicMaps;
import app.organicmaps.sdk.display.DisplayManager;
import app.organicmaps.sdk.location.LocationHelper;
import app.organicmaps.sdk.location.LocationState;
import app.organicmaps.sdk.location.SensorHelper;
import app.organicmaps.sdk.location.TrackRecorder;
import app.organicmaps.sdk.maplayer.isolines.IsolinesManager;
import app.organicmaps.sdk.maplayer.subway.SubwayManager;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.sdk.util.Config;
import app.organicmaps.sdk.util.ConnectionState;
import app.organicmaps.sdk.util.log.Logger;
import app.organicmaps.util.ThemeSwitcher;
import app.organicmaps.util.Utils;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MwmApplication extends Application implements Application.ActivityLifecycleCallbacks
{
  @NonNull
  private static final String TAG = MwmApplication.class.getSimpleName();

  @NonNull
  private final LocationProviderFactoryImpl mLocationProviderFactory = new LocationProviderFactoryImpl();

  @SuppressWarnings("NotNullFieldNotInitialized")
  @NonNull
  private OrganicMaps mOrganicMaps;

  @SuppressWarnings("NotNullFieldNotInitialized")
  @NonNull
  private DisplayManager mDisplayManager;

  @Nullable
  private WeakReference<Activity> mTopActivity;

  @SuppressWarnings("NotNullFieldNotInitialized")
  @NonNull
  public static MwmApplication sInstance;

  @UiThread
  @Nullable
  public Activity getTopActivity()
  {
    return mTopActivity != null ? mTopActivity.get() : null;
  }

  @NonNull
  public SubwayManager getSubwayManager()
  {
    return getOrganicMaps().getSubwayManager();
  }

  @NonNull
  public IsolinesManager getIsolinesManager()
  {
    return getOrganicMaps().getIsolinesManager();
  }

  @NonNull
  public LocationHelper getLocationHelper()
  {
    return getOrganicMaps().getLocationHelper();
  }

  @NonNull
  public SensorHelper getSensorHelper()
  {
    return getOrganicMaps().getSensorHelper();
  }

  @NonNull
  public DisplayManager getDisplayManager()
  {
    return mDisplayManager;
  }

  @NonNull
  public OrganicMaps getOrganicMaps()
  {
    return mOrganicMaps;
  }

  @NonNull
  public LocationProviderFactoryImpl getLocationProviderFactory()
  {
    return mLocationProviderFactory;
  }

  @NonNull
  public static MwmApplication from(@NonNull Context context)
  {
    return (MwmApplication) context.getApplicationContext();
  }

  @NonNull
  public static SharedPreferences prefs(@NonNull Context context)
  {
    return from(context).getOrganicMaps().getPreferences();
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    Logger.i(TAG, "Initializing application");

    sInstance = this;

    // Initialize Koin dependency injection for tut_ui module
    KoinInitializer.initialize(this);

    TutAppFactory.mapListener = new TutMapListener() {


      @Override
      public void startMap(double lat, double lng, float zoom, @NotNull String title, @NotNull Function0<@NotNull Unit> onMapRead, @NotNull Function0<@NotNull Unit> onMapClose, @NotNull String orderId, @NotNull Function1<? super @NotNull String, @NotNull Unit> onStartOrderClicked, @NotNull Function1<? super @NotNull String, @NotNull Unit> onCancelOrderClicked, @NotNull Function1<? super @NotNull String, @NotNull Unit> onCompleteOrderClicked, @NotNull Function0<@NotNull Unit> onShowRouteClicked, @NotNull Function2<? super @NotNull Double, ? super @NotNull Double, @NotNull Unit> onMapClick) {
        // Store callbacks in MapCallbackHolder
        MapCallbackHolder.getInstance().setCallbacks(
            onMapRead,
            onMapClose,
            orderId,
            onStartOrderClicked,
            onCancelOrderClicked,
            onCompleteOrderClicked,
            onShowRouteClicked,
            onMapClick
        );

        // Get current activity context
        Activity activity = getTopActivity();
        if (activity == null) {
          Logger.e(TAG, "Cannot start map: no active activity");
          return;
        }

        // Create intent to start MwmActivity
        android.content.Intent intent = new android.content.Intent(activity, MwmActivity.class);
        intent.putExtra(MwmActivity.EXTRA_MAP_LAT, lat);
        intent.putExtra(MwmActivity.EXTRA_MAP_LNG, lng);
        intent.putExtra(MwmActivity.EXTRA_MAP_ZOOM, zoom);
        intent.putExtra(MwmActivity.EXTRA_MAP_TITLE, title);
        intent.putExtra(MwmActivity.EXTRA_ORDER_ID, orderId);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        // Start MwmActivity
        activity.startActivity(intent);
      }

      @Override
      public void startMapWithRoute(double starLat, double startLng, double endLat, double endLng) {

      }

      @Override
      public void startMapForMapClick(double lat, double lng, @NotNull Function2<? super @NotNull Double, ? super @NotNull Double, @NotNull Unit> onMapClick) {

      }
    };

    PreferenceManager.setDefaultValues(this, R.xml.prefs_main, false);
    mOrganicMaps = new OrganicMaps(getApplicationContext(), BuildConfig.FLAVOR, BuildConfig.APPLICATION_ID,
                                   BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME,
                                   BuildConfig.FILE_PROVIDER_AUTHORITY, mLocationProviderFactory);

    ConnectionState.INSTANCE.initialize(this);

    DownloaderNotifier.createNotificationChannel(this);
    NavigationService.createNotificationChannel(this);
    TrackRecordingService.createNotificationChannel(this);

    registerActivityLifecycleCallbacks(this);
    mDisplayManager = new DisplayManager();
  }

  public boolean initOrganicMaps(@NonNull Runnable onComplete) throws IOException
  {
    ThemeSwitcher.INSTANCE.initialize(this);
    return mOrganicMaps.init(() -> {
      ThemeSwitcher.INSTANCE.restart(false);
      ProcessLifecycleOwner.get().getLifecycle().addObserver(mProcessLifecycleObserver);
      onComplete.run();
    });
  }

  private final LifecycleObserver mProcessLifecycleObserver = new DefaultLifecycleObserver() {
    @Override
    public void onStart(@NonNull LifecycleOwner owner)
    {
      MwmApplication.this.onForeground();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner)
    {
      MwmApplication.this.onBackground();
    }
  };

  @Override
  public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState)
  {}

  @Override
  public void onActivityStarted(@NonNull Activity activity)
  {}

  @Override
  public void onActivityResumed(@NonNull Activity activity)
  {
    Logger.d(TAG, "activity = " + activity);
    Utils.showOnLockScreen(Config.isShowOnLockScreenEnabled(), activity);
    getSensorHelper().setRotation(activity.getWindowManager().getDefaultDisplay().getRotation());
    mTopActivity = new WeakReference<>(activity);
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity)
  {
    Logger.d(TAG, "activity = " + activity);
    mTopActivity = null;
  }

  @Override
  public void onActivityStopped(@NonNull Activity activity)
  {}

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState)
  {
    Logger.d(TAG, "activity = " + activity + " outState = " + outState);
  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity)
  {
    Logger.d(TAG, "activity = " + activity);
  }

  private void onForeground()
  {
    Logger.d(TAG);

    getLocationHelper().resumeLocationInForeground();
  }

  private void onBackground()
  {
    Logger.d(TAG);

    OsmUploadWork.startActionUploadOsmChanges(this);

    if (!mDisplayManager.isDeviceDisplayUsed())
      Logger.i(LOCATION_TAG, "Android Auto is active, keeping location in the background");
    else if (RoutingController.get().isNavigating())
      Logger.i(LOCATION_TAG, "Navigation is in progress, keeping location in the background");
    else if (!Map.isEngineCreated() || LocationState.getMode() == LocationState.PENDING_POSITION)
      Logger.i(LOCATION_TAG, "PENDING_POSITION mode, keeping location in the background");
    else if (TrackRecorder.nativeIsTrackRecordingEnabled())
      Logger.i(LOCATION_TAG, "Track Recordr is active, keeping location in the background");
    else
    {
      Logger.i(LOCATION_TAG, "Stopping location in the background");
      getLocationHelper().stop();
    }
  }
}
