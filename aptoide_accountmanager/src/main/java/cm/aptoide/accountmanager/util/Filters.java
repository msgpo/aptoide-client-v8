/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 26/04/2016.
 */

package cm.aptoide.accountmanager.util;

import cm.aptoide.pt.crashreports.CrashReports;

public class Filters {

  /**
   * TODO: Is this really necessary?? Apart from stupid..
   */
  public enum Screen {
    notfound, small, normal, large, xlarge;

    public static Screen lookup(String screen) {
      try {
        return valueOf(screen);
      } catch (Exception e) {
        CrashReports.logException(e);
        return notfound;
      }
    }
  }
}