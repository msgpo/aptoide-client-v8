package cm.aptoide.pt.download;

import cm.aptoide.analytics.AnalyticsManager;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import java.util.HashMap;

public class AppValidationAnalytics {

  public final static String INVALID_DOWNLOAD_PATH_EVENT = "Invalid_Download_Path";
  private final AnalyticsManager analyticsManager;
  private final NavigationTracker navigationTracker;
  private final String PACKAGE_NAME = "Package_Name";
  private final String FILE_TYPE = "File_Type";
  private final String FILE_TYPE_APK = "apk";
  private final String FILE_TYPE_OBB = "obb";
  private final String FILE_TYPE_LEVEL = "File_Type_Level";
  private final String FILE_TYPE_LEVEL_DOWNLOAD_MAIN = "download_main";
  private final String FILE_TYPE_LEVEL_ALTERNATIVE = "download_alternative";
  private final String FILE_TYPE_LEVEL_OBB_MAIN = "obb_main";
  private final String FILE_TYPE_LEVEL_OBB_PATCH = "obb_patch";

  public AppValidationAnalytics(AnalyticsManager analyticsManager,
      NavigationTracker navigationTracker) {
    this.analyticsManager = analyticsManager;
    this.navigationTracker = navigationTracker;
  }

  private void sendInvalidDownloadPathEvent(String packageName, String fileType,
      String fileTypeLevel) {
    analyticsManager.logEvent(createInvalidPathMap(packageName, fileType, fileTypeLevel),
        INVALID_DOWNLOAD_PATH_EVENT, AnalyticsManager.Action.CLICK, getViewName(true));
  }

  private String getViewName(boolean current) {
    return navigationTracker.getViewName(current);
  }

  private HashMap<String, Object> createInvalidPathMap(String packageName, String fileType,
      String fileTypeLevel) {
    HashMap<String, Object> map = new HashMap<>();
    map.put(PACKAGE_NAME, packageName);
    map.put(FILE_TYPE, fileType);
    map.put(FILE_TYPE_LEVEL, fileTypeLevel);
    return map;
  }
}
