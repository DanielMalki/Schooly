package daniel.malki.schooly;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userListFiltered;
    private List<User> userListFull;

    public UserAdapter(List<User> userList) {
        this.userListFiltered = userList;
        this.userListFull = new ArrayList<>(userList);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userListFiltered.get(position);

        holder.tvUserName.setText(user.getName());
        holder.tvUserId.setText("ID: " + user.getUserId());

        // קביעת טקסט וצבע לתג לפי השדה type
        switch (user.getType()) {
            case 0:
                holder.tvUserRoleBadge.setText("Student");
                holder.tvUserRoleBadge.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case 1:
                holder.tvUserRoleBadge.setText("Teacher");
                holder.tvUserRoleBadge.setBackgroundColor(Color.parseColor("#FF9800"));
                break;
            case 2:
                holder.tvUserRoleBadge.setText("School Admin");
                holder.tvUserRoleBadge.setBackgroundColor(Color.parseColor("#E91E63"));
                break;
            case 3:
                holder.tvUserRoleBadge.setText("Schooly Admin");
                holder.tvUserRoleBadge.setBackgroundColor(Color.parseColor("#673AB7"));
                break;
        }

        // טעינת תמונת הפרופיל מה-Blob
        FirebaseFirestore.getInstance().collection("users").document(user.getUserId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getBlob("profileImageBlob") != null) {
                        byte[] imageBytes = doc.getBlob("profileImageBlob").toBytes();
                        Glide.with(holder.itemView.getContext())
                                .load(imageBytes)
                                .circleCrop()
                                .into(holder.imgUserAvatar);
                    } else {
                        holder.imgUserAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                }).addOnFailureListener(e -> {
                    holder.imgUserAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                });

        // 🌟 הפיצ'ר החדש: מאזין ללחיצה על כל השורה
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), UserDetailActivity.class);
            intent.putExtra("selectedUserId", user.getUserId()); // העברת ה-ID למסך הבא

            // 🔥 התיקון: בודקים אם ה-Context הוא אכן האקטיביטי שלנו, ומפעילים דרכה את הלאנצ'ר
            if (holder.itemView.getContext() instanceof ManageUsersActivity) {
                ManageUsersActivity activity = (ManageUsersActivity) holder.itemView.getContext();
                activity.editUserLauncher.launch(intent);
            } else {
                // גיבוי ליתר ביטחון, אם ה-Context איכשהו שונה
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userListFiltered != null ? userListFiltered.size() : 0;
    }

    public void updateList(List<User> newList) {
        this.userListFiltered = newList;
        this.userListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query, int roleFilter) {
        List<User> filteredList = new ArrayList<>();

        for (User user : userListFull) {
            boolean matchesQuery = user.getName().toLowerCase().contains(query.toLowerCase()) ||
                    user.getUserId().contains(query);

            boolean matchesRole = false;

            if (roleFilter == 0) {
                matchesRole = true; // All Roles
            } else if (roleFilter == 1 && user.getType() == 0) {
                matchesRole = true; // Students (type 0)
            } else if (roleFilter == 2 && user.getType() == 1) {
                matchesRole = true; // Teachers (type 1)
            } else if (roleFilter == 3 && user.getType() == 2) {
                matchesRole = true; // School Admins (type 2)
            } else if (roleFilter == 4 && user.getType() == 3) {
                matchesRole = true; // Schooly Admins (type 3)
            }

            if (matchesQuery && matchesRole) {
                filteredList.add(user);
            }
        }

        // ✅ תיקון: הצבה למשתנה הנכון שמוגדר בראש המחלקה
        this.userListFiltered = filteredList;
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUserAvatar;
        TextView tvUserName, tvUserId, tvUserRoleBadge;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserId = itemView.findViewById(R.id.tvUserId);
            tvUserRoleBadge = itemView.findViewById(R.id.tvUserRoleBadge);
        }
    }
}