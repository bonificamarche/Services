// IAidlServerServiceCallback.aidl
package it.bonificamarche.services.aidl;
import it.bonificamarche.services.aidl.Transmission;
import it.bonificamarche.services.aidl.Photo;
import it.bonificamarche.services.aidl.Action;

interface IAidlServerServiceCallback {
    oneway void sendStatusTransmissionPhoto(in Action action, in Transmission transmission, in Photo photo, String message);
}