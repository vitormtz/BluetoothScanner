package com.example.bluetoothscanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDeviceInfo> {

    private Context context;
    private List<BluetoothDeviceInfo> deviceList;

    public DeviceListAdapter(Context context, List<BluetoothDeviceInfo> deviceList) {
        super(context, R.layout.item_device, deviceList);
        this.context = context;
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_device, parent, false);

            holder = new ViewHolder();
            holder.deviceNameTextView = convertView.findViewById(R.id.deviceNameTextView);
            holder.signalStrengthTextView = convertView.findViewById(R.id.signalStrengthTextView);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BluetoothDeviceInfo device = deviceList.get(position);

        holder.deviceNameTextView.setText(device.getName());

        String signalText = "Sinal: " + device.getSignalStrength() + " dBm";
        holder.signalStrengthTextView.setText(signalText);

        return convertView;
    }

    private static class ViewHolder {
        TextView deviceNameTextView;
        TextView signalStrengthTextView;
    }
}
