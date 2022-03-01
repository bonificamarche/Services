// IAidlServerServiceCallback.aidl
package it.bonificamarche.services;

interface IAidlServerServiceCallback {
    oneway void sendMsg(String msg);
}