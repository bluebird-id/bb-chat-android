package id.bluebird.chat.sdk.demos.message;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import id.bluebird.chat.R;

public class MessagesPredefinedAdapter extends ArrayAdapter<String> {

    public MessagesPredefinedAdapter(Context context, String[] messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String message = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(
                    getContext()).inflate(R.layout.message_predefined, parent, false);

        }

        TextView tvName = convertView.findViewById(R.id.tv_message_predefined);

        tvName.setText(message);

        return convertView;

    }
}
