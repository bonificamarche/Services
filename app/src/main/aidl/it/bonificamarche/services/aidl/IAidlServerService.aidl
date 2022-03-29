// IAidlServerService.aidl
package it.bonificamarche.services.aidl;
import it.bonificamarche.services.aidl.IAidlServerServiceCallback;
import it.bonificamarche.services.aidl.Transmission;
import it.bonificamarche.services.aidl.Photo;
import it.bonificamarche.services.aidl.Action;

interface IAidlServerService {

    int getPid();

    void startSendPhoto(String path, int id);
    void stopSendPhoto();

    void notifyClient(in Action action, in Transmission transmission, in Photo photo, String notifyContent);

    // Callbacks
    void registerCallback(IAidlServerServiceCallback cb);
    void unregisterCallback(IAidlServerServiceCallback cb);
}