
package com.ruptech.tttalk_android.bus;

public class NetChangeEvent {
    public final boolean connectivity;

    public NetChangeEvent(boolean connectivity) {
        this.connectivity = connectivity;
    }
}
