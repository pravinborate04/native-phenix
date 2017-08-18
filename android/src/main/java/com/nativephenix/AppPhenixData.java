package com.nativephenix;

import android.media.projection.MediaProjectionManager;

import com.phenixp2p.pcast.PCast;

/**
 * Created by Pravin Borate on 16/8/17.
 */

public class AppPhenixData
{
   public static MediaProjectionManager projectionManager = null;
   public static boolean isStopPublish = false;
   public static boolean isLandscape=false;
   public static boolean isShare=false;
   public static boolean isBackground=false;
   public static PCast pCast=null;
   public static int positionUriMenu=0;
   public static String serverAddress=null;
   public static String pcastAddress=null;
}
