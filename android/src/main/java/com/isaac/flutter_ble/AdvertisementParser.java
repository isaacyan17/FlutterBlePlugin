package com.isaac.flutter_ble;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import com.google.protobuf.ByteString;
import com.isaac.flutter_ble.Protos.AdvertisementData;

public class AdvertisementParser {

    /**
     * Parses packet data into {@link AdvertisementData} structure.
     *
     * @param rawData The scan record data.
     * @return An AdvertisementData proto object.
     * @throws ArrayIndexOutOfBoundsException if the input is truncated.
     */
    static AdvertisementData parse(byte[] rawData) {
        ByteBuffer data = ByteBuffer.wrap(rawData).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        AdvertisementData.Builder ret = AdvertisementData.newBuilder();
        boolean seenLongLocalName = false;
        do {
            int length = data.get() & 0xFF;
            if (length == 0) {
                break;
            }
            if (length > data.remaining()) {
                throw new ArrayIndexOutOfBoundsException("Not enough data.");
            }

            int type = data.get() & 0xFF;
            length--;

            switch (type) {
                case 0x08: // Short local name.
                case 0x09: { // Long local name.
                    if (seenLongLocalName) {
                        // Prefer the long name over the short.
                        data.position(data.position() + length);
                        break;
                    }
                    byte[] name = new byte[length];
                    data.get(name);
                    try {
                        ret.setLocalName(new String(name, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    if (type == 0x09) {
                        seenLongLocalName = true;
                    }
                    break;
                }
                case 0x0A: { // Power level.
                    ret.setTxPowerLevel(Protos.Int32Value.newBuilder().setValue(data.get()));
                    break;
                }
                case 0x16: // Service Data with 16 bit UUID.
                case 0x20: // Service Data with 32 bit UUID.
                case 0x21: { // Service Data with 128 bit UUID.
                    UUID uuid;
                    int remainingDataLength = 0;
                    if (type == 0x16 || type == 0x20) {
                        long uuidValue;
                        if (type == 0x16) {
                            uuidValue = data.getShort() & 0xFFFF;
                            remainingDataLength = length - 2;
                        } else {
                            uuidValue = data.getInt() & 0xFFFFFFFF;
                            remainingDataLength = length - 4;
                        }
                        uuid = UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuidValue));
                    } else {
                        long msb = data.getLong();
                        long lsb = data.getLong();
                        uuid = new UUID(msb, lsb);
                        remainingDataLength = length - 16;
                    }
                    byte[] remainingData = new byte[remainingDataLength];
                    data.get(remainingData);
                    ret.putServiceData(uuid.toString(), ByteString.copyFrom(remainingData));
                    break;
                }
                case 0xFF: {// Manufacturer specific data.
                    if (length < 2) {
                        throw new ArrayIndexOutOfBoundsException("Not enough data for Manufacturer specific data.");
                    }
                    int manufacturerId = data.getShort();
                    if ((length - 2) > 0) {
                        byte[] msd = new byte[length - 2];
                        data.get(msd);
                        ret.putManufacturerData(manufacturerId, ByteString.copyFrom(msd));
                    }
                    break;
                }
                default: {
                    data.position(data.position() + length);
                    break;
                }
            }
        } while (true);
        return ret.build();
    }
}
