package com.example.bluetoothscanner;

public class BluetoothDeviceInfo {
    private String name;
    private int signalStrength;

    public BluetoothDeviceInfo(String name, int signalStrength) {
        this.name = (name != null && !name.isEmpty()) ? name : "Dispositivo Desconhecido";
        this.signalStrength = signalStrength;
    }

    public String getName() {
        return name;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BluetoothDeviceInfo)) {
            return false;
        }
        BluetoothDeviceInfo other = (BluetoothDeviceInfo) obj;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

