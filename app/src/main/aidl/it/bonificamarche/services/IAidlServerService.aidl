// IAidlServerService.aidl
package it.bonificamarche.services;
import it.bonificamarche.services.IAidlServerServiceCallback;
import it.bonificamarche.services.Transmission;
import it.bonificamarche.services.Photo;

interface IAidlServerService {

    int getPid();

    void startSendPhoto(String path, int id);
    void stopSendPhoto();

    void notifyClient(in Transmission transmission, in Photo photo, String notifyContent);

    // Callbacks
    void registerCallback(IAidlServerServiceCallback cb);
    void unregisterCallback(IAidlServerServiceCallback cb);
}