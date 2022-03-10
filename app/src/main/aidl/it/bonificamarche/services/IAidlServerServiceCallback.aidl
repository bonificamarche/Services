// IAidlServerServiceCallback.aidl
package it.bonificamarche.services;
import it.bonificamarche.services.Transmission;
import it.bonificamarche.services.Photo;

interface IAidlServerServiceCallback {
    oneway void sendStatusTransmissionPhoto(in Transmission transmission, in Photo photo);
}