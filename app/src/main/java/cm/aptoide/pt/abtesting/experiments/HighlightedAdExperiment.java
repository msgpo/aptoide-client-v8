package cm.aptoide.pt.abtesting.experiments;

import cm.aptoide.pt.BuildConfig;
import cm.aptoide.pt.abtesting.ABTestManager;
import cm.aptoide.pt.app.AdsManager;
import cm.aptoide.pt.app.AppNextAdResult;
import com.appnext.core.AppnextError;
import rx.Observable;
import rx.Scheduler;
import rx.Single;

public class HighlightedAdExperiment {

  public static final String DEFAULT_ASSIGNMENT_ERROR = "Default_AB_Assignment";
  private final String EXPERIMENT_ID = "ASV-1106-AppNext_Highlighted";

  private ABTestManager abTestManager;
  private Scheduler scheduler;
  private AdsManager adsManager;

  public HighlightedAdExperiment(ABTestManager abTestManager, Scheduler scheduler,
      AdsManager adsManager) {
    this.abTestManager = abTestManager;
    this.scheduler = scheduler;
    this.adsManager = adsManager;
  }

  public Single<AppNextAdResult> getAppNextAd() {
    return abTestManager.getExperiment(EXPERIMENT_ID)
        .observeOn(scheduler)
        .flatMapSingle(experiment -> {
          String experimentAssignment = "default";
          if (!experiment.isExperimentOver() && experiment.isPartOfExperiment()) {
            experimentAssignment = experiment.getAssignment();
          }
          switch (experimentAssignment) {
            case "appnext_ad":
              return adsManager.loadAppNextAd(null,
                  BuildConfig.APPNEXT_HIGHLIGHTED_ABTEST_PLACEMENT_ID);
            case "default":
            case "no_appnext_ad":
            default:
              return Single.just(new AppNextAdResult(new AppnextError(DEFAULT_ASSIGNMENT_ERROR)));
          }
        })
        .toSingle();
  }

  public Observable<Boolean> recordAdImpression() {
    return abTestManager.recordImpression(EXPERIMENT_ID);
  }

  public Observable<Boolean> recordAdClick() {
    return abTestManager.recordAction(EXPERIMENT_ID);
  }
}
