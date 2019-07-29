package cm.aptoide.pt.view.configuration.implementation;

import androidx.appcompat.app.AppCompatActivity;
import cm.aptoide.pt.view.ActivityProvider;
import cm.aptoide.pt.view.MainActivity;

public class VanillaActivityProvider implements ActivityProvider {
  @Override public Class<? extends AppCompatActivity> getMainActivityFragmentClass() {
    return MainActivity.class;
  }
}
