package it.bonificamarche.services.aidl

import android.os.Parcel
import android.os.Parcelable
import it.bonificamarche.services.Actions

/**
 * Photo parcelable.
 */
class Photo(
    val name: String?,
    val fullName: String?
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        name = parcel.readString(),
        fullName = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(fullName)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "Photo: name = $name, fullName = $fullName"
    }

    companion object CREATOR : Parcelable.Creator<Photo> {
        override fun createFromParcel(parcel: Parcel) = Photo(parcel)

        override fun newArray(size: Int): Array<Photo?> {
            return arrayOfNulls(size)
        }
    }
}


/**
 * Transmission parcelable.
 */
class Transmission(
    val src: String?,
    val photoToBeTransmitted: Int,
    var photoTransmitted: Int
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(src)
        parcel.writeInt(photoToBeTransmitted)
        parcel.writeInt(photoTransmitted)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "Transmission: src = $src, photoToBeTransmitted = $photoToBeTransmitted," +
                " photoTransmitted = $photoTransmitted"
    }

    companion object CREATOR : Parcelable.Creator<Transmission> {
        override fun createFromParcel(parcel: Parcel): Transmission {
            return Transmission(parcel)
        }

        override fun newArray(size: Int): Array<Transmission?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Action parcelable.
 */
class Action(
    val action: Actions
):Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readSerializable() as Actions
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(action)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Action> {
        override fun createFromParcel(parcel: Parcel): Action {
            return Action(parcel)
        }

        override fun newArray(size: Int): Array<Action?> {
            return arrayOfNulls(size)
        }
    }
}

