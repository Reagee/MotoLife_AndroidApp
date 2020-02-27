package com.app.motolife.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.motolife.model.Chat;
import com.bumptech.glide.Glide;
import com.example.motolife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<Chat> mChat;
    private String imageURL;

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageURL) {
        this.mChat = mChat;
        this.mContext = mContext;
        this.imageURL = imageURL;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                viewType == MSG_TYPE_RIGHT
                        ? LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false)
                        : LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Chat chat = mChat.get(position);

        holder.show_message.setText(chat.getMessage());
        if (imageURL.equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(imageURL).into(holder.profile_image);
        }

        if (position == mChat.size() - 1)
            holder.msg_seen.setText(chat.isIsseen() ? "Seen" : "Delivered");
        else
            holder.msg_seen.setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView show_message;
        private ImageView profile_image;
        private TextView msg_seen;

        ViewHolder(View itemView) {
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            msg_seen = itemView.findViewById(R.id.msg_seen);
        }
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        return Objects.equals(mChat.get(position).getSender(), firebaseUser.getUid()) ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
    }
}
