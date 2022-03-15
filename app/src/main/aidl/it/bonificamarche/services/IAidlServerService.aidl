// IAidlServerService.aidl
package it.bonificamarche.services;
import it.bonificamarche.services.IAidlServerServiceCallback;

interface IAidlServerService {

    int getPid();

    void startSendPhoto(String path);
    void stopSendPhoto();

    void notifyClient(String notifyContent);

    // Callbacks
    void registerCallback(IAidlServerServiceCallback cb);
    void unregisterCallback(IAidlServerServiceCallback cb);
}