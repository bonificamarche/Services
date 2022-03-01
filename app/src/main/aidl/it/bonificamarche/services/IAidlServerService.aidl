// IAidlServerService.aidl
package it.bonificamarche.services;
import it.bonificamarche.services.IAidlServerServiceCallback;

interface IAidlServerService {

    int getPid();

    void sendPhoto(String path);
    void notifyClient(String notifyContent);

    // Callbacks
    void registerCallback(IAidlServerServiceCallback cb);
    void unregisterCallback(IAidlServerServiceCallback cb);
}