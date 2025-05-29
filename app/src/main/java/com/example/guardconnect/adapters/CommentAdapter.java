package com.example.guardconnect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guardconnect.R;
import com.example.guardconnect.api.models.Comment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private static final String TAG = "CommentAdapter";
    private List<Comment> commentList;
    private Context context;
    private static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault());

    public CommentAdapter(List<Comment> commentList, Context context) {
        this.commentList = commentList;
        this.context = context;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        
        // Définir l'auteur du commentaire
        holder.tvAuthor.setText(comment.getUserName());
        
        // Formater et définir la date
        String formattedDate = formatDate(comment.getCreatedAt());
        holder.tvDate.setText(formattedDate);
        
        // Définir le texte du commentaire
        holder.tvComment.setText(comment.getComment());
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }
    
    private String formatDate(String dateString) {
        try {
            if (dateString != null) {
                Date date = INPUT_FORMAT.parse(dateString);
                if (date != null) {
                    return OUTPUT_FORMAT.format(date);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "Date inconnue";
    }
    
    /**
     * Ajoute un commentaire à la liste et notifie l'adapter
     */
    public void addComment(Comment comment) {
        commentList.add(0, comment);
        notifyItemInserted(0);
    }
    
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor;
        TextView tvDate;
        TextView tvComment;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tv_comment_author);
            tvDate = itemView.findViewById(R.id.tv_comment_date);
            tvComment = itemView.findViewById(R.id.tv_comment_text);
        }
    }
} 