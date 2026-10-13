package com.xtraword.apps.meetminder.cosmetics;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class UniformSpacingDecoration extends RecyclerView.ItemDecoration {
    private final int spacing;
    private final int spanCount;

    public UniformSpacingDecoration(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (this.spanCount == 1) {
            outRect.left = this.spacing;
            outRect.right = this.spacing;
            outRect.bottom = this.spacing;
            if (position == 0) {
                outRect.top = this.spacing;
                return;
            }
            return;
        }
        int column = position % this.spanCount;
        outRect.left = this.spacing - ((this.spacing * column) / this.spanCount);
        outRect.right = ((column + 1) * this.spacing) / this.spanCount;
        if (position < this.spanCount) {
            outRect.top = this.spacing;
        }
        outRect.bottom = this.spacing;
    }
}
