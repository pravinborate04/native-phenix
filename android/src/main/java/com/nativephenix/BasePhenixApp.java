package com.nativephenix;

import android.media.projection.MediaProjectionManager;
import android.text.TextUtils;

import com.phenixp2p.pcast.PCast;

/**
 * Created by Pravin Borate on 16/8/17.
 */

public interface BasePhenixApp
{
     MediaProjectionManager projectionManager = null;
     boolean isStopPublish = false;
     boolean isLandscape=false;
     boolean isShare=false;
     boolean isBackground=false;
     PCast pCast=null;
     int positionUriMenu=0;
     String serverAddress=null;
     String pcastAddress=null;


}
