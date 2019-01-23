package cm.aptoide.pt;

import cm.aptoide.pt.abtesting.ABTestManager;
import cm.aptoide.pt.abtesting.experiments.MoPubBannerAdExperiment;
import cm.aptoide.pt.abtesting.experiments.MoPubInterstitialAdExperiment;
import cm.aptoide.pt.ads.MoPubAnalytics;
import cm.aptoide.pt.view.FragmentScope;
import dagger.Module;
import dagger.Provides;

@Module public class FlavourFragmentModule {

  public FlavourFragmentModule() {
  }

  @FragmentScope @Provides MoPubInterstitialAdExperiment providesMoPubInterstitialAdExperiment(
      ABTestManager abTestManager, MoPubAnalytics moPubAnalytics) {
    return new MoPubInterstitialAdExperiment(abTestManager, moPubAnalytics);
  }

  @FragmentScope @Provides MoPubBannerAdExperiment providesMoPubBannerAdExperiment(
      ABTestManager abTestManager, MoPubAnalytics moPubAnalytics) {
    return new MoPubBannerAdExperiment(abTestManager, moPubAnalytics);
  }
}
