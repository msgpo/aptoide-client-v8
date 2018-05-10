package cm.aptoide.pt.app.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import cm.aptoide.pt.R;
import cm.aptoide.pt.analytics.ScreenTagHistory;
import cm.aptoide.pt.app.AdsViewModel;
import cm.aptoide.pt.app.DetailedAppViewModel;
import cm.aptoide.pt.app.ReviewsViewModel;
import cm.aptoide.pt.app.view.screenshots.NewScreenshotsAdapter;
import cm.aptoide.pt.app.view.screenshots.ScreenShotClickEvent;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.model.v7.GetAppMeta;
import cm.aptoide.pt.dataprovider.model.v7.Malware;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.permission.DialogPermissions;
import cm.aptoide.pt.reviews.LanguageFilterHelper;
import cm.aptoide.pt.store.StoreTheme;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.app.DetailedApp;
import cm.aptoide.pt.view.dialog.DialogBadgeV7;
import cm.aptoide.pt.view.fragment.BaseToolbarFragment;
import com.jakewharton.rxbinding.view.RxView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import rx.Single;
import rx.subjects.PublishSubject;

/**
 * Created by franciscocalado on 07/05/18.
 */

public class NewAppViewFragment extends BaseToolbarFragment implements AppViewView {
  private static final String ORIGIN_TAG = "TAG";
  private static final String BADGE_DIALOG_TAG = "badgeDialog";

  @Inject AppViewPresenter presenter;
  private Menu menu;
  private long appId;
  private String packageName;
  private NewScreenshotsAdapter screenshotsAdapter;
  private PublishSubject<ScreenShotClickEvent> screenShotClick;
  private PublishSubject<ReadMoreClickEvent> readMoreClick;

  //Views
  private ImageView appIcon;
  private TextView appName;
  private View trustedLayout;
  private ImageView trustedBadge;
  private TextView trustedText;
  private TextView downloadsTop;
  private TextView sizeInfo;
  private Button installButton;
  private TextView appcValue;
  private TextView latestVersion;
  private TextView otherVersions;
  private RecyclerView screenshots;
  private TextView descriptionText;
  private Button descriptionReadMore;
  private TextView reviewUsers;
  private TextView avgReviewScore;
  private RecyclerView commentsView;
  private Button rateAppButton;
  private Button showAllCommentsButton;

  private View goodAppLayoutWrapper;
  private View flagsLayoutWrapper;
  private View workingWellLayout;
  private View needsLicenseLayout;
  private View fakeAppLayout;
  private View virusLayout;
  private TextView workingWellText;
  private TextView needsLicenceText;
  private TextView fakeAppText;
  private TextView virusText;
  private View storeLayout;
  private ImageView storeIcon;
  private TextView storeName;
  private TextView storeFollowers;
  private TextView storeDownloads;
  private Button storeFollow;
  private RecyclerView similarApps;
  private TextView infoWebsite;
  private TextView infoEmail;
  private TextView infoPrivacy;
  private TextView infoPermissions;

  private ProgressBar viewProgress;
  private View appview;

  public static NewAppViewFragment newInstance(long appId, String packageName,
      AppViewFragment.OpenType openType, String tag) {
    Bundle bundle = new Bundle();
    bundle.putString(ORIGIN_TAG, tag);
    bundle.putLong(AppViewFragment.BundleKeys.APP_ID.name(), appId);
    bundle.putString(AppViewFragment.BundleKeys.PACKAGE_NAME.name(), packageName);
    bundle.putSerializable(AppViewFragment.BundleKeys.SHOULD_INSTALL.name(), openType);
    NewAppViewFragment fragment = new NewAppViewFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getFragmentComponent(savedInstanceState).inject(this);
    screenShotClick = PublishSubject.create();
    readMoreClick = PublishSubject.create();
  }

  @Override public void loadExtras(Bundle args) {
    super.loadExtras(args);

    appId = args.getLong(AppViewFragment.BundleKeys.APP_ID.name(), -1);
    packageName = args.getString(AppViewFragment.BundleKeys.PACKAGE_NAME.name(), null);
  }

  @Override public void setupViews() {
    super.setupViews();

    screenshotsAdapter =
        new NewScreenshotsAdapter(new ArrayList<>(), new ArrayList<>(), screenShotClick);
    screenshots.setAdapter(screenshotsAdapter);

    attachPresenter(presenter);
  }

  @Override public void bindViews(View view) {
    super.bindViews(view);

    appIcon = (ImageView) view.findViewById(R.id.app_icon);
    trustedBadge = (ImageView) view.findViewById(R.id.trusted_badge);
    appName = (TextView) view.findViewById(R.id.app_name);
    trustedLayout = view.findViewById(R.id.trusted_layout);
    trustedText = (TextView) view.findViewById(R.id.trusted_text);
    downloadsTop = (TextView) view.findViewById(R.id.header_downloads);
    sizeInfo = (TextView) view.findViewById(R.id.header_size);
    installButton = (Button) view.findViewById(R.id.install_button);
    appcValue = (TextView) view.findViewById(R.id.appc_layout)
        .findViewById(R.id.appcoins_reward_message);
    latestVersion = (TextView) view.findViewById(R.id.versions_layout)
        .findViewById(R.id.latest_version);
    otherVersions = (TextView) view.findViewById(R.id.other_versions);

    screenshots = (RecyclerView) view.findViewById(R.id.screenshots_list);
    screenshots.setLayoutManager(
        new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
    screenshots.setNestedScrollingEnabled(false);

    descriptionText = (TextView) view.findViewById(R.id.description_text);
    descriptionReadMore = (Button) view.findViewById(R.id.description_see_more);
    reviewUsers = (TextView) view.findViewById(R.id.users_voted);
    avgReviewScore = (TextView) view.findViewById(R.id.rating_value);
    commentsView = (RecyclerView) view.findViewById(R.id.top_comments_list);
    rateAppButton = (Button) view.findViewById(R.id.rate_this_button);
    showAllCommentsButton = (Button) view.findViewById(R.id.read_all_button);

    goodAppLayoutWrapper = view.findViewById(R.id.good_app_layout);
    flagsLayoutWrapper = view.findViewById(R.id.rating_flags_layout);
    workingWellLayout = view.findViewById(R.id.working_well_layout);
    needsLicenseLayout = view.findViewById(R.id.needs_licence_layout);
    fakeAppLayout = view.findViewById(R.id.fake_app_layout);
    virusLayout = view.findViewById(R.id.virus_layout);

    workingWellText = (TextView) view.findViewById(R.id.working_well_count);
    needsLicenceText = (TextView) view.findViewById(R.id.needs_licence_count);
    fakeAppText = (TextView) view.findViewById(R.id.fake_app_count);
    virusText = (TextView) view.findViewById(R.id.virus_count);
    storeLayout = view.findViewById(R.id.store_uploaded_layout);
    storeIcon = (ImageView) view.findViewById(R.id.store_icon);
    storeName = (TextView) view.findViewById(R.id.store_name);
    storeFollowers = (TextView) view.findViewById(R.id.user_count);
    storeDownloads = (TextView) view.findViewById(R.id.download_count);
    storeFollow = (Button) view.findViewById(R.id.follow_button);
    similarApps = (RecyclerView) view.findViewById(R.id.similar_list);
    infoWebsite = (TextView) view.findViewById(R.id.website_label);
    infoEmail = (TextView) view.findViewById(R.id.email_label);
    infoPrivacy = (TextView) view.findViewById(R.id.privacy_policy_label);
    infoPermissions = (TextView) view.findViewById(R.id.permissions_label);

    viewProgress = (ProgressBar) view.findViewById(R.id.appview_progress);
    appview = view.findViewById(R.id.appview_full);
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build("AppViewFragment", "", StoreContext.meta);
  }

  @Override public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    this.menu = menu;
    inflater.inflate(R.menu.fragment_appview, menu);
  }

  @Override public int getContentViewId() {
    return R.layout.fragment_new_app_view;
  }

  @Override public void showLoading() {
    appview.setVisibility(View.GONE);
    viewProgress.setVisibility(View.VISIBLE);
  }

  @Override public void showAppview() {
    appview.setVisibility(View.VISIBLE);
    viewProgress.setVisibility(View.GONE);
  }

  @Override public long getAppId() {
    return appId;
  }

  @Override public String getPackageName() {
    return packageName;
  }

  @Override public void populateAppDetails(DetailedAppViewModel detailedApp) {
    StoreTheme storeThemeEnum = StoreTheme.get(detailedApp.getDetailedApp()
        .getStore());

    appName.setText(detailedApp.getDetailedApp()
        .getName());
    ImageLoader.with(getContext())
        .load(detailedApp.getDetailedApp()
            .getIcon(), appIcon);
    downloadsTop.setText(String.format("%s", AptoideUtils.StringU.withSuffix(
        detailedApp.getDetailedApp()
            .getStats()
        .getPdownloads())));
    sizeInfo.setText(AptoideUtils.StringU.formatBytes(detailedApp.getDetailedApp()
        .getSize(), false));
    latestVersion.setText(detailedApp.getDetailedApp()
        .getFile()
        .getVername());
    storeName.setText(detailedApp.getDetailedApp()
        .getStore()
        .getName());
    ImageLoader.with(getContext())
        .loadWithShadowCircleTransform(detailedApp.getDetailedApp()
            .getStore()
            .getAvatar(), storeIcon);
    storeDownloads.setText(String.format("%s", AptoideUtils.StringU.withSuffix(
        detailedApp.getDetailedApp()
            .getStore()
            .getStats()
            .getDownloads())));
    storeFollowers.setText(String.format("%s", AptoideUtils.StringU.withSuffix(
        detailedApp.getDetailedApp()
            .getStore()
            .getStats()
            .getSubscribers())));
    storeFollow.setBackgroundDrawable(
        storeThemeEnum.getButtonLayoutDrawable(getResources(), getContext().getTheme()));
    if ((detailedApp.getDetailedApp()
        .getMedia()
        .getScreenshots() != null && !detailedApp.getDetailedApp()
        .getMedia()
        .getScreenshots()
        .isEmpty()) || (detailedApp.getDetailedApp()
        .getMedia()
        .getVideos() != null && !detailedApp.getDetailedApp()
        .getMedia()
        .getVideos()
        .isEmpty())) {
      screenshotsAdapter.updateScreenshots(detailedApp.getDetailedApp()
          .getMedia()
          .getScreenshots());
      screenshotsAdapter.updateVideos(detailedApp.getDetailedApp()
          .getMedia()
          .getVideos());
    } else {
      screenshots.setVisibility(View.GONE);
    }
    setTrustedBadge(detailedApp.getDetailedApp());
    setDescription(detailedApp.getDetailedApp()
        .getMedia()
        .getDescription());
    setAppFlags(detailedApp.getDetailedApp()
        .getFile());
    setReadMoreClickListener(detailedApp.getDetailedApp());
    setDeveloperDetails(detailedApp.getDetailedApp());
    showAppview();
  }

  @Override public Observable<ScreenShotClickEvent> getScreenshotClickEvent() {
    return screenShotClick;
  }

  @Override public Observable<ReadMoreClickEvent> clickedReadMore() {
    return readMoreClick;
  }

  @Override public Single<Void> populateReviewsAndAds(ReviewsViewModel reviews, AdsViewModel ads) {
    return Single.just(null);
  }

  @Override public Observable<Void> clickWorkingFlag() {
    return RxView.clicks(workingWellLayout);
  }

  @Override public Observable<Void> clickLicenseFlag() {
    return RxView.clicks(needsLicenseLayout);
  }

  @Override public Observable<Void> clickFakeFlag() {
    return RxView.clicks(fakeAppLayout);
  }

  @Override public Observable<Void> clickVirusFlag() {
    return RxView.clicks(virusLayout);
  }

  @Override public void displayNotLoggedInSnack() {

  }

  @Override public void displayStoreFollowedSnack(String storeName) {
    String messageToDisplay = String.format(getString(R.string.store_followed), storeName);
    Toast.makeText(getContext(), messageToDisplay, Toast.LENGTH_SHORT)
        .show();
  }

  @Override public Observable<Void> clickDeveloperWebsite() {
    return RxView.clicks(infoWebsite);
  }

  @Override public Observable<Void> clickDeveloperEmail() {
    return RxView.clicks(infoEmail);
  }

  @Override public Observable<Void> clickDeveloperPrivacy() {
    return RxView.clicks(infoPrivacy);
  }

  @Override public Observable<Void> clickDeveloperPermissions() {
    return RxView.clicks(infoPermissions);
  }

  @Override public Observable<Void> clickStoreLayout() {
    return RxView.clicks(storeLayout);
  }

  @Override public Observable<Void> clickFollowStore() {
    return RxView.clicks(storeFollow);
  }

  @Override public Observable<Void> clickOtherVersions() {
    return RxView.clicks(otherVersions);
  }

  @Override public Observable<Void> clickTrustedBadge() {
    return RxView.clicks(trustedLayout);
  }

  @Override public void navigateToDeveloperWebsite(DetailedApp app) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.getDeveloper()
        .getWebsite()));
    getContext().startActivity(browserIntent);
  }

  @Override public void navigateToDeveloperEmail(DetailedApp app) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    Uri data = Uri.parse("mailto:" + app.getDeveloper()
        .getEmail() + "?subject=" + "Feedback" + "&body=" + "");
    intent.setData(data);
    getContext().startActivity(intent);
  }

  @Override public void navigateToDeveloperPrivacy(DetailedApp app) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.getDeveloper()
        .getPrivacy()));
    getContext().startActivity(browserIntent);
  }

  @Override public void navigateToDeveloperPermissions(DetailedApp app) {
    DialogPermissions dialogPermissions = DialogPermissions.newInstance(app);
    dialogPermissions.show(getActivity().getSupportFragmentManager(), "");
  }

  @Override public void setFollowButton(boolean isFollowing) {
    if (isFollowing) {
      storeFollow.setText(R.string.follow);
    } else {
      storeFollow.setText(R.string.followed);
    }
  }

  @Override public void showTrustedDialog(DetailedApp app) {
    DialogBadgeV7.newInstance(app.getFile()
        .getMalware(), app.getName(), app.getFile()
        .getMalware()
        .getRank())
        .show(getFragmentManager(), BADGE_DIALOG_TAG);
  }

  @Override public String getLanguageFilter() {
    List<String> countryCodes =
        new LanguageFilterHelper(getContext().getResources()).getCurrentLanguageFirst()
            .getCountryCodes();
    return countryCodes.get(0);
  }

  private void setTrustedBadge(DetailedApp app) {
    @DrawableRes int badgeResId;
    @StringRes int badgeMessageId;

    Malware.Rank rank = app.getFile()
        .getMalware()
        .getRank() == null ? Malware.Rank.UNKNOWN : app.getFile()
        .getMalware()
        .getRank();
    switch (rank) {
      case TRUSTED:
        badgeResId = R.drawable.ic_badge_trusted;
        badgeMessageId = R.string.appview_header_trusted_text;
        break;

      case WARNING:
        badgeResId = R.drawable.ic_badge_warning;
        badgeMessageId = R.string.warning;
        break;

      case CRITICAL:
        badgeResId = R.drawable.ic_badge_critical;
        badgeMessageId = R.string.critical;
        break;

      default:
      case UNKNOWN:
        badgeResId = R.drawable.ic_badge_unknown;
        badgeMessageId = R.string.unknown;
        break;
    }
    Drawable icon = ContextCompat.getDrawable(getContext(), badgeResId);
    trustedBadge.setImageDrawable(icon);
    trustedText.setText(badgeMessageId);
  }

  private void setDescription(String description) {
    if (!TextUtils.isEmpty(description)) {
      descriptionText.setText(AptoideUtils.HtmlU.parse(description));
    } else {
      // only show "default" description if the app doesn't have one
      descriptionText.setText(R.string.description_not_available);
      descriptionReadMore.setVisibility(View.GONE);
    }
  }

  private void setReadMoreClickListener(DetailedApp detailedApp) {
    descriptionReadMore.setOnClickListener(view -> readMoreClick.onNext(
        new ReadMoreClickEvent(detailedApp.getName(), detailedApp.getMedia()
            .getDescription(), detailedApp.getStore()
            .getAppearance()
            .getTheme())));
  }

  private void setAppFlags(GetAppMeta.GetAppMetaFile file) {
    if (file.isGoodApp()) {
      goodAppLayoutWrapper.setVisibility(View.VISIBLE);
      flagsLayoutWrapper.setVisibility(View.GONE);
    } else {
      goodAppLayoutWrapper.setVisibility(View.GONE);
      flagsLayoutWrapper.setVisibility(View.VISIBLE);
      setFlagValues(file);
    }
  }

  private void setFlagValues(GetAppMeta.GetAppMetaFile file) {
    try {
      GetAppMeta.GetAppMetaFile.Flags flags = file.getFlags();

      if (flags != null && flags.getVotes() != null && !flags.getVotes()
          .isEmpty()) {
        for (final GetAppMeta.GetAppMetaFile.Flags.Vote vote : flags.getVotes()) {
          applyCount(vote.getType(), vote.getCount());
        }
      }
    } catch (NullPointerException ex) {
      CrashReport.getInstance()
          .log(ex);
    }
  }

  private void applyCount(GetAppMeta.GetAppMetaFile.Flags.Vote.Type type, int count) {
    String countAsString = Integer.toString(count);
    switch (type) {
      case GOOD:
        workingWellText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case VIRUS:
        virusText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case FAKE:
        fakeAppText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case LICENSE:
        needsLicenceText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case FREEZE:
        break;

      default:
        throw new IllegalArgumentException("Unable to find Type " + type.name());
    }
  }

  private void setDeveloperDetails(DetailedApp app) {
    if (!TextUtils.isEmpty(app.getDeveloper()
        .getWebsite())) {
      String website = app.getDeveloper()
          .getWebsite();
      String websiteCompositeString = String.format(getString(R.string.developer_website), website);
      SpannableString compositeSpan = new SpannableString(websiteCompositeString);
      compositeSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)),
          websiteCompositeString.indexOf(website),
          websiteCompositeString.indexOf(website) + website.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      infoWebsite.setText(compositeSpan);
    } else {
      infoWebsite.setText(
          String.format(getString(R.string.developer_website), getString(R.string.not_available)));
    }

    if (!TextUtils.isEmpty(app.getDeveloper()
        .getEmail())) {
      String email = app.getDeveloper()
          .getEmail();
      String emailCompositeString = String.format(getString(R.string.developer_email), email);
      SpannableString compositeSpan = new SpannableString(emailCompositeString);
      compositeSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)),
          emailCompositeString.indexOf(email), emailCompositeString.indexOf(email) + email.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      infoEmail.setText(compositeSpan);
    } else {
      infoEmail.setText(
          String.format(getString(R.string.developer_email), getString(R.string.not_available)));
    }

    if (!TextUtils.isEmpty(app.getDeveloper()
        .getPrivacy())) {
      String privacy = app.getDeveloper()
          .getPrivacy();
      String privacyCompositeString =
          String.format(getString(R.string.developer_privacy_policy), privacy);
      SpannableString compositeSpan = new SpannableString(privacyCompositeString);
      compositeSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)),
          privacyCompositeString.indexOf(privacy),
          privacyCompositeString.indexOf(privacy) + privacy.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      infoPrivacy.setText(compositeSpan);
    } else {
      infoPrivacy.setText(String.format(getString(R.string.developer_privacy_policy),
          getString(R.string.not_available)));
    }
  }
}