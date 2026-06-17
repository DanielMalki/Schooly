package daniel.malki.schooly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public UserAdapter(List<User> userList) {
        this.userList = new ArrayList<>(userList);
    }

    public void updateList(List<User> newList) {
        this.userList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvUserName.setText(user.getName());
        holder.tvUserId.setText("ID: " + user.getUserId());

        if (user.getType() == 0) {
            holder.tvUserRoleBadge.setText("Student");
            holder.tvUserRoleBadge.setBackgroundResource(android.R.color.holo_green_dark);
        } else if (user.getType() == 1) {
            holder.tvUserRoleBadge.setText("Teacher");
            holder.tvUserRoleBadge.setBackgroundResource(android.R.color.holo_orange_dark);
        } else if (user.getType() == 2) {
            holder.tvUserRoleBadge.setText("School Admin");
            holder.tvUserRoleBadge.setBackgroundResource(android.R.color.holo_blue_dark);
        } else if (user.getType() == 3) {
            holder.tvUserRoleBadge.setText("System Admin");
            holder.tvUserRoleBadge.setBackgroundResource(android.R.color.holo_red_dark);
        }

        if (user.isExceptionStudent()) {
            holder.imgExceptionWarning.setVisibility(View.VISIBLE);
        } else {
            holder.imgExceptionWarning.setVisibility(View.GONE);
        }

        if (user.getProfileImageBlob() != null) {
            try {
                byte[] bytes = user.getProfileImageBlob().toBytes();
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.profileImageBlob.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                // החזרת האייקון הדיפולטיבי המקורי למקרה של שגיאה בפענוח
                holder.profileImageBlob.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        } else {
            // החזרת האייקון הדיפולטיבי המקורי כשאין למשתמש תמונה בדאטהבייס
            holder.profileImageBlob.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageBlob, imgExceptionWarning;
        TextView tvUserName, tvUserId, tvUserRoleBadge;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageBlob = itemView.findViewById(R.id.profileImageBlob);
            imgExceptionWarning = itemView.findViewById(R.id.imgExceptionWarning);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserId = itemView.findViewById(R.id.tvUserId);
            tvUserRoleBadge = itemView.findViewById(R.id.tvUserRoleBadge);
        }
    }
}