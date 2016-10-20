/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 02/09/2016.
 */

package cm.aptoide.pt.v8engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.ws.responses.Subscription;
import cm.aptoide.pt.crashreports.CrashReports;
import cm.aptoide.pt.database.accessors.AccessorFactory;
import cm.aptoide.pt.database.accessors.Database;
import cm.aptoide.pt.database.accessors.DownloadAccessor;
import cm.aptoide.pt.database.accessors.InstalledAccessor;
import cm.aptoide.pt.database.accessors.StoreAccessor;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.database.realm.Store;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.repository.IdsRepository;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.downloadmanager.CacheHelper;
import cm.aptoide.pt.downloadmanager.DownloadService;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.preferences.PRNGFixes;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import cm.aptoide.pt.preferences.secure.SecurePreferences;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.FileUtils;
import cm.aptoide.pt.utils.SecurityUtils;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.configuration.ActivityProvider;
import cm.aptoide.pt.v8engine.configuration.FragmentProvider;
import cm.aptoide.pt.v8engine.configuration.implementation.ActivityProviderImpl;
import cm.aptoide.pt.v8engine.configuration.implementation.FragmentProviderImpl;
import cm.aptoide.pt.v8engine.deprecated.SQLiteDatabaseHelper;
import cm.aptoide.pt.v8engine.download.TokenHttpClient;
import cm.aptoide.pt.v8engine.util.StoreUtils;
import cm.aptoide.pt.v8engine.util.UpdateUtils;
import cm.aptoide.pt.v8engine.view.recycler.DisplayableWidgetMapping;
import com.flurry.android.FlurryAgent;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by neuro on 14-04-2016.
 */
public abstract class V8Engine extends DataProvider {

  private static final String TAG = V8Engine.class.getName();

  @Getter static DownloadService downloadService;
  @Getter private static FragmentProvider fragmentProvider;
  @Getter private static ActivityProvider activityProvider;
  @Getter private static DisplayableWidgetMapping displayableWidgetMapping;
  private RefWatcher refWatcher;

  public static void loadStores() {

    AptoideAccountManager.getUserRepos().subscribe(subscriptions -> {

      if (subscriptions.size() > 0) {
        for (Subscription subscription : subscriptions) {
          Store store = new Store();

          store.setDownloads(Long.parseLong(subscription.getDownloads()));
          store.setIconPath(subscription.getAvatarHd() != null ? subscription.getAvatarHd()
              : subscription.getAvatar());
          store.setStoreId(subscription.getId().longValue());
          store.setStoreName(subscription.getName());
          store.setTheme(subscription.getTheme());

          ((StoreAccessor) AccessorFactory.getAccessorFor(Store.class)).insert(store);
        }
      } else {
        addDefaultStore();
      }

      UpdateUtils.checkUpdates();
    }, e -> {
      Logger.e(TAG, e);
      //CrashReports.logException(e);
    });
  }

  public static void loadUserData() {
    loadStores();
  }

  public static void clearUserData() {
    AccessorFactory.getAccessorFor(Store.class).removeAll();
    StoreUtils.subscribeStore(getConfiguration().getDefaultStore(), null, null);
  }

  public static RefWatcher getRefWatcher(Context context) {
    V8Engine app = (V8Engine) context.getApplicationContext();
    return app.refWatcher;
  }

  private static void addDefaultStore() {
    StoreUtils.subscribeStore(getConfiguration().getDefaultStore(),
        getStoreMeta -> UpdateUtils.checkUpdates(), null);
  }

  @Override public void onCreate() {
    CrashReports.setup(this);
    try {
      PRNGFixes.apply();
    } catch (Exception e) {
      Logger.e(TAG, "onCreate: " + e);
      CrashReports.logException(e);
    }
    long l = System.currentTimeMillis();
    AptoideUtils.setContext(this);
    fragmentProvider = createFragmentProvider();
    activityProvider = createActivityProvider();
    displayableWidgetMapping = createDisplayableWidgetMapping();

    //
    // super
    //
    super.onCreate();

    //if (BuildConfig.DEBUG) {
    //  RxJavaPlugins.getInstance().registerObservableExecutionHook(new RxJavaStackTracer());
    //}

    Database.initialize(this);

    generateAptoideUUID().subscribe();

    SecurePreferences.setUserAgent(AptoideUtils.NetworkUtils.getDefaultUserAgent(
        new IdsRepository(SecurePreferencesImplementation.getInstance(), this),
        AptoideAccountManager.getUserEmail()));

    SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(this);
    Analytics.LocalyticsSessionControl.firstSession(sPref);
    Analytics.Lifecycle.Application.onCreate(this);
    Logger.setDBG(ManagerPreferences.isDebug() || cm.aptoide.pt.utils.BuildConfig.DEBUG);
    new FlurryAgent.Builder().withLogEnabled(false).build(this, BuildConfig.FLURRY_KEY);

    if (BuildConfig.DEBUG) {
      refWatcher = LeakCanary.install(this);
      //registerActivityLifecycleCallbacks(new LeakCAnaryActivityWatcher(refWatcher));
      Logger.w(TAG, "LeakCanary installed");
    } else {
      refWatcher = RefWatcher.DISABLED;
    }

    if (SecurePreferences.isFirstRun()) {
      PreferenceManager.setDefaultValues(this, R.xml.settings, false);
      loadInstalledApps().doOnNext(o -> {
        if (AptoideAccountManager.isLoggedIn()) {

          if (!SecurePreferences.isUserDataLoaded()) {
            loadUserData();
            SecurePreferences.setUserDataLoaded();
          }
        } else {
          generateAptoideUUID().subscribe(success -> addDefaultStore());
        }
        SecurePreferences.setFirstRun(false);
      }).subscribe();

      // load picture, name and email
      AptoideAccountManager.refreshAndSaveUserInfoData().subscribe(userData -> {
        Logger.v(TAG, "hello " + userData.getUsername());
      }, e -> {
        Logger.e(TAG, e);
      });
    }

    final int appSignature = SecurityUtils.checkAppSignature(this);
    if (appSignature != SecurityUtils.VALID_APP_SIGNATURE) {
      Logger.e(TAG, "app signature is not valid!");
    }

    if (SecurityUtils.checkEmulator()) {
      Logger.w(TAG, "application is running on an emulator");
    }

    if (SecurityUtils.checkDebuggable(this)) {
      Logger.w(TAG, "application has debug flag active");
    }

    final DownloadAccessor downloadAccessor = AccessorFactory.getAccessorFor(Download.class);
    final DownloadManagerSettingsI settingsInterface = new DownloadManagerSettingsI();
    AptoideDownloadManager.getInstance()
        .init(this, new DownloadNotificationActionsActionsInterface(), settingsInterface,
            downloadAccessor, new CacheHelper(downloadAccessor, settingsInterface),
            new FileUtils(action -> Analytics.File.moveFile(action)), new TokenHttpClient(
                new IdsRepository(SecurePreferencesImplementation.getInstance(), this),
                AptoideAccountManager.getUserEmail()));

    // setupCurrentActivityListener();

    if (BuildConfig.DEBUG) {
      setupStrictMode();
      Logger.w(TAG, "StrictMode setup");
    }

    // this will trigger the migration if needed
    // FIXME: 24/08/16 sithengineer the following line should be removed when no more SQLite -> Realm migration is needed
    SQLiteDatabase db = new SQLiteDatabaseHelper(this).getWritableDatabase();
    db.close();

    Logger.d(TAG, "onCreate took " + (System.currentTimeMillis() - l) + " millis.");
  }

  protected FragmentProvider createFragmentProvider() {
    return new FragmentProviderImpl();
  }

  protected ActivityProvider createActivityProvider() {
    return new ActivityProviderImpl();
  }

  protected DisplayableWidgetMapping createDisplayableWidgetMapping() {
    return DisplayableWidgetMapping.getInstance();
  }

  Observable<String> generateAptoideUUID() {
    return Observable.fromCallable(
        () -> new IdsRepository(SecurePreferencesImplementation.getInstance(),
            this).getAptoideClientUUID()).subscribeOn(Schedulers.computation());
  }

  private Observable<?> loadInstalledApps() {
    return Observable.fromCallable(() -> {
      InstalledAccessor installedAccessor = AccessorFactory.getAccessorFor(Installed.class);
      installedAccessor.removeAll();

      List<PackageInfo> installedApps = AptoideUtils.SystemU.getAllInstalledApps();
      Logger.d(TAG, "Found " + installedApps.size() + " user installed apps.");

      // Installed apps are inserted in database based on their firstInstallTime. Older comes first.
      Collections.sort(installedApps,
          (lhs, rhs) -> (int) ((lhs.firstInstallTime - rhs.firstInstallTime) / 1000));

      for (PackageInfo packageInfo : installedApps) {
        Installed installed = new Installed(packageInfo);
        installedAccessor.insert(installed);
      }
      return null;
    }).subscribeOn(Schedulers.io());
  }

  //
  // Strict Mode
  //

  private void setupStrictMode() {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads()
        .detectDiskWrites()
        .detectNetwork()   // or .detectAll() for all detectable problems
        .penaltyLog()
        .build());

    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
        .detectLeakedClosableObjects()
        .penaltyLog()
        .penaltyDeath()
        .build());
  }

  //	private static class LeakCAnaryActivityWatcher implements ActivityLifecycleCallbacks {
  //
  //		private final RefWatcher refWatcher;
  //
  //		private LeakCAnaryActivityWatcher(RefWatcher refWatcher) {
  //			this.refWatcher = refWatcher;
  //		}
  //
  //		@Override
  //		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
  //
  //		}
  //
  //		@Override
  //		public void onActivityStarted(Activity activity) {
  //
  //		}
  //
  //		@Override
  //		public void onActivityResumed(Activity activity) {
  //
  //		}
  //
  //		@Override
  //		public void onActivityPaused(Activity activity) {
  //
  //		}
  //
  //		@Override
  //		public void onActivityStopped(Activity activity) {
  //
  //		}
  //
  //		@Override
  //		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
  //
  //		}
  //
  //		@Override
  //		public void onActivityDestroyed(Activity activity) {
  //			refWatcher.watch(activity);
  //		}
  //	}

  @Override protected TokenInvalidator getTokenInvalidator() {
    return AptoideAccountManager::invalidateAccessToken;
  }
}
