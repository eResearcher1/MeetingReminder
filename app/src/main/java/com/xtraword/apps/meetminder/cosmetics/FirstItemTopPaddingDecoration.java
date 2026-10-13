package com.xtraword.apps.meetminder.cosmetics;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class FirstItemTopPaddingDecoration extends RecyclerView.ItemDecoration {
    private final boolean applyToBothItems;
    private final int topMargin;

    public FirstItemTopPaddingDecoration(Context context, int topMargin, boolean applyToBothItems) {
        this.topMargin = topMargin;
        this.applyToBothItems = applyToBothItems;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            if (position == 0 || (this.applyToBothItems && position == 1)) {
                outRect.top = this.topMargin;
                return;
            }
            return;
        }
        if ((layoutManager instanceof LinearLayoutManager) && position == 0) {
            outRect.top = this.topMargin;
        }
    }
}
