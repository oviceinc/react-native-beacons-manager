package com.mackentoch.beaconsandroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BeaconsAndroidModule extends ReactContextBaseJavaModule {
  private static final String LOG_TAG = "BeaconsAndroidModule";
  private static final int RUNNING_AVG_RSSI_FILTER = 0;
  private static final int ARMA_RSSI_FILTER = 1;
  private BeaconManager mBeaconManager;
  private Context mApplicationContext;
  private ReactApplicationContext mReactContext;

  static final String ERROR_INVALID_CONFIG = "ERROR_INVALID_CONFIG";

  public BeaconsAndroidModule(ReactApplicationContext reactContext) {
    super(reactContext);
    Log.d(LOG_TAG, "BeaconsAndroidModule - started");
    this.mReactContext = reactContext;
  }

  @Override
  public void initialize() {
    this.mApplicationContext = this.mReactContext.getApplicationContext();
    this.mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);
    mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
    mBeaconManager.addMonitorNotifier(mMonitorNotifier);
    mBeaconManager.addRangeNotifier(mRangeNotifier);
    sendEvent(mReactContext, "beaconServiceConnected", null);
  }

  @Override
  public String getName() {
    return LOG_TAG;
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("SUPPORTED", BeaconTransmitter.SUPPORTED);
    constants.put("NOT_SUPPORTED_MIN_SDK", BeaconTransmitter.NOT_SUPPORTED_MIN_SDK);
    constants.put("NOT_SUPPORTED_BLE", BeaconTransmitter.NOT_SUPPORTED_BLE);
    constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS);
    constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER);
    constants.put("RUNNING_AVG_RSSI_FILTER", RUNNING_AVG_RSSI_FILTER);
    constants.put("ARMA_RSSI_FILTER", ARMA_RSSI_FILTER);
    return constants;
  }

  @ReactMethod
  public void setHardwareEqualityEnforced(Boolean e) {
    Beacon.setHardwareEqualityEnforced(e.booleanValue());
  }

  @ReactMethod
  public void addParser(String parser, Callback resolve, Callback reject) {
    try {
      Log.d(LOG_TAG, "BeaconsAndroidModule - addParser: " + parser);
      mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(parser));
      resolve.invoke();
    } catch (Exception e) {
      reject.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void removeParser(String parser, Callback resolve, Callback reject) {
    try {
      Log.d(LOG_TAG, "BeaconsAndroidModule - removeParser: " + parser);
      mBeaconManager.getBeaconParsers().remove(new BeaconParser().setBeaconLayout(parser));
      resolve.invoke();
    } catch (Exception e) {
      reject.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void addParsersListToDetection(ReadableArray parsers, Callback resolve, Callback reject) {
    try {
      for (int i = 0; i < parsers.size(); i++) {
        String parser = parsers.getString(i);
        Log.d(LOG_TAG, "addParsersListToDetection - add parser: " + parser);
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(parser));
      }
      resolve.invoke(parsers);
    } catch (Exception e) {
      reject.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void removeParsersListToDetection(ReadableArray parsers, Callback resolve, Callback reject) {
    try {
      for (int i = 0; i < parsers.size(); i++) {
        String parser = parsers.getString(i);
        Log.d(LOG_TAG, "removeParsersListToDetection - remove parser: " + parser);
        mBeaconManager.getBeaconParsers().remove(new BeaconParser().setBeaconLayout(parser));
      }
      resolve.invoke(parsers);
    } catch (Exception e) {
      reject.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void setBackgroundScanPeriod(int period) {
    mBeaconManager.setBackgroundScanPeriod((long) period);
  }

  @ReactMethod
  public void setBackgroundBetweenScanPeriod(int period) {
    mBeaconManager.setBackgroundBetweenScanPeriod((long) period);
  }

  @ReactMethod
  public void setForegroundScanPeriod(int period) {
    mBeaconManager.setForegroundScanPeriod((long) period);
  }

  @ReactMethod
  public void setForegroundBetweenScanPeriod(int period) {
    mBeaconManager.setForegroundBetweenScanPeriod((long) period);
  }

  @ReactMethod
  public void setRssiFilter(int filterType, double avgModifier) {
    String logMsg = "Could not set the rssi filter.";
    if (filterType == RUNNING_AVG_RSSI_FILTER) {
      logMsg = "Setting filter RUNNING_AVG";
      BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
      if (avgModifier > 0) {
        RunningAverageRssiFilter.setSampleExpirationMilliseconds((long) avgModifier);
        logMsg += " with custom avg modifier";
      }
    } else if (filterType == ARMA_RSSI_FILTER) {
      logMsg = "Setting filter ARMA";
      BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
      if (avgModifier > 0) {
        ArmaRssiFilter.setDEFAULT_ARMA_SPEED(avgModifier);
        logMsg += " with custom avg modifier";
      }
    }
    Log.d(LOG_TAG, logMsg);
  }

  @ReactMethod
  public void checkTransmissionSupported(Callback callback) {
    int result = BeaconTransmitter.checkTransmissionSupported(mReactContext);
    callback.invoke(result);
  }

  @ReactMethod
  public void getMonitoredRegions(Callback callback) {
    WritableArray array = new WritableNativeArray();
    for (Region region : mBeaconManager.getMonitoredRegions()) {
      WritableMap map = new WritableNativeMap();
      map.putString("identifier", region.getUniqueId());
      map.putString("uuid", region.getId1().toString());
      map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
      map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
      array.pushMap(map);
    }
    callback.invoke(array);
  }

  @ReactMethod
  public void getRangedRegions(Callback callback) {
    WritableArray array = new WritableNativeArray();
    for (Region region : mBeaconManager.getRangedRegions()) {
      WritableMap map = new WritableNativeMap();
      map.putString("region", region.getUniqueId());
      map.putString("uuid", region.getId1().toString());
      array.pushMap(map);
    }
    callback.invoke(array);
  }

  /***********************************************************************************************
   * Monitoring
   **********************************************************************************************/
  @ReactMethod
  public void startMonitoring(String regionId, String beaconUuid, int minor, int major, Callback resolve, Callback reject) {
    Log.d(LOG_TAG, "startMonitoring, monitoringRegionId: " + regionId + ", monitoringBeaconUuid: " + beaconUuid + ", minor: " + minor + ", major: " + major);
    try {
      Region region = createRegion(
        regionId,
        beaconUuid,
        String.valueOf(minor).equals("-1") ? "" : String.valueOf(minor),
        String.valueOf(major).equals("-1") ? "" : String.valueOf(major)
      );
      mBeaconManager.startMonitoring(region);
      resolve.invoke();
    } catch (Exception e) {
      Log.e(LOG_TAG, "startMonitoring, error: ", e);
      reject.invoke(e.getMessage());
    }
  }

  private MonitorNotifier mMonitorNotifier = new MonitorNotifier() {
    @Override
    public void didEnterRegion(Region region) {
      sendEvent(mReactContext, "regionDidEnter", createMonitoringResponse(region));
    }

    @Override
    public void didExitRegion(Region region) {
      sendEvent(mReactContext, "regionDidExit", createMonitoringResponse(region));
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
      String state = "unknown";
      switch (i) {
        case MonitorNotifier.INSIDE:
          state = "inside";
          break;
        case MonitorNotifier.OUTSIDE:
          state = "outside";
          break;
        default:
          break;
      }
      WritableMap map = createMonitoringResponse(region);
      map.putString("state", state);
      sendEvent(mReactContext, "didDetermineState", map);
    }
  };

  private WritableMap createMonitoringResponse(Region region) {
    WritableMap map = new WritableNativeMap();
    map.putString("identifier", region.getUniqueId());
    map.putString("uuid", region.getId1() != null ? region.getId1().toString() : "");
    map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
    map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
    return map;
  }

  @ReactMethod
  public void stopMonitoring(String regionId, String beaconUuid, int minor, int major, Callback resolve, Callback reject) {
    Region region = createRegion(
      regionId,
      beaconUuid,
      String.valueOf(minor).equals("-1") ? "" : String.valueOf(minor),
      String.valueOf(major).equals("-1") ? "" : String.valueOf(major)
      // minor,
      // major
    );

    try {
      mBeaconManager.stopMonitoring(region);
      resolve.invoke();
    } catch (Exception e) {
      Log.e(LOG_TAG, "stopMonitoring, error: ", e);
      reject.invoke(e.getMessage());
    }
  }

  /***********************************************************************************************
   * Ranging
   **********************************************************************************************/
  @ReactMethod
  public void startRanging(String regionId, String beaconUuid, Callback resolve, Callback reject) {
    Log.d(LOG_TAG, "startRanging, rangingRegionId: " + regionId + ", rangingBeaconUuid: " + beaconUuid);
    Region region = createRegion(regionId, beaconUuid);
    mBeaconManager.startRangingBeacons(region);
    resolve.invoke();
  }

  private RangeNotifier mRangeNotifier = new RangeNotifier() {
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
      Log.d(LOG_TAG, "rangingConsumer didRangeBeaconsInRegion, beacons: " + beacons.toString());
      Log.d(LOG_TAG, "rangingConsumer didRangeBeaconsInRegion, region: " + region.toString());
      sendEvent(mReactContext, "beaconsDidRange", createRangingResponse(beacons, region));
    }
  };

  private WritableMap createRangingResponse(Collection<Beacon> beacons, Region region) {
    WritableMap map = new WritableNativeMap();
    map.putString("identifier", region.getUniqueId());
    map.putString("uuid", region.getId1() != null ? region.getId1().toString() : "");
    WritableArray a = new WritableNativeArray();
    for (Beacon beacon : beacons) {
      WritableMap b = new WritableNativeMap();
      b.putString("uuid", beacon.getId1().toString());
      if (beacon.getIdentifiers().size() > 2) {
        b.putInt("major", beacon.getId2().toInt());
        b.putInt("minor", beacon.getId3().toInt());
      }
      b.putInt("rssi", beacon.getRssi());
      if (beacon.getDistance() == Double.POSITIVE_INFINITY
        || Double.isNaN(beacon.getDistance())
        || beacon.getDistance() == Double.NaN
        || beacon.getDistance() == Double.NEGATIVE_INFINITY) {
        b.putDouble("distance", 999.0);
        b.putString("proximity", "far");
      } else {
        b.putDouble("distance", beacon.getDistance());
        b.putString("proximity", getProximity(beacon.getDistance()));
      }
      a.pushMap(b);
    }
    map.putArray("beacons", a);
    return map;
  }

  private String getProximity(double distance) {
    if (distance == -1.0) {
      return "unknown";
    } else if (distance < 1) {
      return "immediate";
    } else if (distance < 3) {
      return "near";
    } else {
      return "far";
    }
  }

  @ReactMethod
  public void stopRanging(String regionId, String beaconUuid, Callback resolve, Callback reject) {
    Region region = createRegion(regionId, beaconUuid);
    try {
      mBeaconManager.stopRangingBeacons(region);
      resolve.invoke();
    } catch (Exception e) {
      Log.e(LOG_TAG, "stopRanging, error: ", e);
      reject.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void requestStateForRegion(String regionId, String beaconUuid, int minor, int major) {
    Region region = createRegion(
      regionId,
      beaconUuid,
      String.valueOf(minor).equals("-1") ? "" : String.valueOf(minor),
      String.valueOf(major).equals("-1") ? "" : String.valueOf(major)
    );
    mBeaconManager.requestStateForRegion(region);
  }


  /***********************************************************************************************
   * Utils
   **********************************************************************************************/
  private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
    if (reactContext.hasActiveCatalystInstance()) {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
    }
  }

  private Region createRegion(String regionId, String beaconUuid) {
    Identifier id1 = (beaconUuid == null) ? null : Identifier.parse(beaconUuid);
    return new Region(regionId, id1, null, null);
  }

  private Region createRegion(String regionId, String beaconUuid, String minor, String major) {
    Identifier id1 = (beaconUuid == null) ? null : Identifier.parse(beaconUuid);
    return new Region(
      regionId,
      id1,
      major.length() > 0 ? Identifier.parse(major) : null,
      minor.length() > 0 ? Identifier.parse(minor) : null
    );
  }

  @ReactMethod
  public void enableForegroundServiceScanning(ReadableMap notificationConfig, Promise promise) {
    if (notificationConfig == null) {
      promise.reject(ERROR_INVALID_CONFIG, "Notification config is invalid");
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (!notificationConfig.hasKey("channelId")) {
        promise.reject(ERROR_INVALID_CONFIG, "channelId is required");
        return;
      }
    }

    if (!notificationConfig.hasKey("id")) {
      promise.reject(ERROR_INVALID_CONFIG , "id is required");
      return;
    }

    if (!notificationConfig.hasKey("icon")) {
      promise.reject(ERROR_INVALID_CONFIG, "icon is required");
      return;
    }

    if (!notificationConfig.hasKey("title")) {
      promise.reject(ERROR_INVALID_CONFIG, "title is reqired");
      return;
    }

    if (!notificationConfig.hasKey("text")) {
      promise.reject(ERROR_INVALID_CONFIG, "text is required");
      return;
    }

    Notification notification = buildNotification(mApplicationContext, notificationConfig);
    if (notification == null) {
      promise.reject(ERROR_INVALID_CONFIG, "unable to build notification");
      return;
    }

    mBeaconManager.enableForegroundServiceScanning(notification, (int)notificationConfig.getDouble("id"));
    promise.resolve(null);
  }

  Notification buildNotification(Context context, @NonNull ReadableMap notificationConfig) {
    Class mainActivityClass = getMainActivityClass(context);
    if (mainActivityClass == null) {
      return null;
    }
    Intent notificationIntent = new Intent(context, mainActivityClass);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
      (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? (PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT) : PendingIntent.FLAG_UPDATE_CURRENT);

    Notification.Builder notificationBuilder;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String channelId = notificationConfig.getString("channelId");
      if (channelId == null) {
        Log.e(LOG_TAG, "buildNotification: invalid channelId");
        return null;
      }
      notificationBuilder = new Notification.Builder(context, channelId);
    } else {
      notificationBuilder = new Notification.Builder(context);
    }

    int priorityInt = notificationConfig.hasKey("priority") ? notificationConfig.getInt("priority"): Notification.PRIORITY_HIGH;

    int priority;
    switch (priorityInt) {
      case 0:
        priority = Notification.PRIORITY_DEFAULT;
        break;
      case -1:
        priority = Notification.PRIORITY_LOW;
        break;
      case -2:
        priority = Notification.PRIORITY_MIN;
        break;
      case 2:
        priority = Notification.PRIORITY_MAX;
        break;
      case 1:
      default:
        priority = Notification.PRIORITY_HIGH;
        break;

    }

    notificationBuilder.setContentTitle(notificationConfig.getString("title"))
      .setContentText(notificationConfig.getString("text"))
      .setPriority(priority)
      .setContentIntent(pendingIntent);

    String iconName = notificationConfig.getString("icon");
    if (iconName != null) {
      notificationBuilder.setSmallIcon(getResourceIdForResourceName(context, iconName));
    }

    return notificationBuilder.build();
  }

  private Class getMainActivityClass(Context context) {
    String packageName = context.getPackageName();
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    if (launchIntent == null || launchIntent.getComponent() == null) {
      Log.e("NotificationHelper", "Failed to get launch intent or component");
      return null;
    }
    try {
      return Class.forName(launchIntent.getComponent().getClassName());
    } catch (ClassNotFoundException e) {
      Log.e("NotificationHelper", "Failed to get main activity class");
      return null;
    }
  }

  private int getResourceIdForResourceName(Context context, String resourceName) {
    int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
    if (resourceId == 0) {
      resourceId = context.getResources().getIdentifier(resourceName, "mipmap", context.getPackageName());
    }
    return resourceId;
  }
}
