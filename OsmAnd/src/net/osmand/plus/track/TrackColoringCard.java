package net.osmand.plus.track;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.GpxAppearanceAdapter.AppearanceListItem;
import net.osmand.plus.dialogs.GpxAppearanceAdapter.GpxAppearanceAdapterType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.widgets.FlowLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.dialogs.GpxAppearanceAdapter.getAppearanceItems;

public class TrackColoringCard extends BaseCard {

	private TrackDrawInfo trackDrawInfo;
	private SelectedGpxFile selectedGpxFile;

	private GradientScaleType selectedScaleType;

	private TrackColoringAdapter coloringAdapter;

	@ColorInt
	private int selectedColor;

	public TrackColoringCard(MapActivity mapActivity, SelectedGpxFile selectedGpxFile, TrackDrawInfo trackDrawInfo) {
		super(mapActivity);
		this.trackDrawInfo = trackDrawInfo;
		this.selectedGpxFile = selectedGpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_coloring_card;
	}

	@Override
	protected void updateContent() {
		updateHeader();
		createColorSelector();
		updateColorSelector();

		coloringAdapter = new TrackColoringAdapter(Arrays.asList(GradientScaleType.values()));
		RecyclerView groupRecyclerView = view.findViewById(R.id.recycler_view);
		groupRecyclerView.setAdapter(coloringAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.top_divider), isShowDivider());
	}

	private void createColorSelector() {
		FlowLayout selectColor = view.findViewById(R.id.select_color);
		List<Integer> colors = new ArrayList<>();
		for (AppearanceListItem appearanceListItem : getAppearanceItems(app, GpxAppearanceAdapterType.TRACK_COLOR)) {
			if (!colors.contains(appearanceListItem.getColor())) {
				colors.add(appearanceListItem.getColor());
			}
		}
		for (int color : colors) {
			selectColor.addView(createColorItemView(color, selectColor), new FlowLayout.LayoutParams(0, 0));
		}

		if (selectedGpxFile.isShowCurrentTrack()) {
			selectedColor = app.getSettings().CURRENT_TRACK_COLOR.get();
		} else {
			selectedColor = selectedGpxFile.getGpxFile().getColor(0);
		}
		updateColorSelector(selectedColor, selectColor);
	}

	private View createColorItemView(@ColorInt final int color, final FlowLayout rootView) {
		FrameLayout colorItemView = (FrameLayout) UiUtilities.getInflater(rootView.getContext(), nightMode)
				.inflate(R.layout.point_editor_button, rootView, false);
		ImageView outline = colorItemView.findViewById(R.id.outline);
		outline.setImageDrawable(
				UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.bg_point_circle_contour),
						ContextCompat.getColor(app,
								nightMode ? R.color.stroked_buttons_and_links_outline_dark
										: R.color.stroked_buttons_and_links_outline_light)));
		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);
		backgroundCircle.setImageDrawable(UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, R.drawable.bg_point_circle), color));
		backgroundCircle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateColorSelector(color, rootView);
				coloringAdapter.notifyDataSetChanged();
			}
		});
		colorItemView.setTag(color);
		return colorItemView;
	}

	private void updateColorSelector(int color, View rootView) {
		View oldColor = rootView.findViewWithTag(selectedColor);
		if (oldColor != null) {
			oldColor.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView icon = oldColor.findViewById(R.id.icon);
			icon.setImageDrawable(UiUtilities.tintDrawable(icon.getDrawable(), R.color.icon_color_default_light));
		}
		View newColor = rootView.findViewWithTag(color);
		if (newColor != null) {
			newColor.findViewById(R.id.outline).setVisibility(View.VISIBLE);
		}
		selectedColor = color;
		trackDrawInfo.setColor(color);
		mapActivity.refreshMap();
	}

	private GradientScaleType getSelectedScaleType() {
		if (selectedScaleType == null) {
			selectedScaleType = trackDrawInfo.getGradientScaleType();
			if (selectedScaleType == null) {
				selectedScaleType = GradientScaleType.SOLID;
			}
		}
		return selectedScaleType;
	}

	private void updateHeader() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.select_color);

		TextView descriptionView = view.findViewById(R.id.description);
		descriptionView.setText(getSelectedScaleType().getHumanString(view.getContext()));
	}

	private void updateColorSelector() {
		boolean visible = GradientScaleType.SOLID == getSelectedScaleType();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.select_color), visible);
	}

	private class TrackColoringAdapter extends RecyclerView.Adapter<TrackColoringViewHolder> {

		private List<GradientScaleType> items;

		private TrackColoringAdapter(List<GradientScaleType> items) {
			this.items = items;
		}

		@NonNull
		@Override
		public TrackColoringViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater themedInflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			View view = themedInflater.inflate(R.layout.point_editor_group_select_item, parent, false);
			view.getLayoutParams().width = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_width);
			view.getLayoutParams().height = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_height);

			TrackColoringViewHolder holder = new TrackColoringViewHolder(view);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, holder.button, nightMode, R.drawable.ripple_solid_light_6dp,
						R.drawable.ripple_solid_dark_6dp);
			}
			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull final TrackColoringViewHolder holder, int position) {
			GradientScaleType item = items.get(position);
			holder.colorKey.setText(item.getHumanString(holder.itemView.getContext()));

			updateButtonBg(holder, item);

			int colorId;
			if (item == GradientScaleType.SOLID) {
				colorId = trackDrawInfo.getColor();
			} else if (item.equals(getSelectedScaleType())) {
				colorId = ContextCompat.getColor(app, nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light);
			} else {
				colorId = AndroidUtils.getColorFromAttr(holder.itemView.getContext(), R.attr.default_icon_color);
			}

			holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(item.getIconId(), colorId));

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int prevSelectedPosition = getItemPosition(getSelectedScaleType());
					selectedScaleType = items.get(holder.getAdapterPosition());
					notifyItemChanged(holder.getAdapterPosition());
					notifyItemChanged(prevSelectedPosition);

					trackDrawInfo.setGradientScaleType(selectedScaleType);
					mapActivity.refreshMap();

					updateHeader();
					updateColorSelector();
				}
			});
		}

		private void updateButtonBg(TrackColoringViewHolder holder, GradientScaleType item) {
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				if (getSelectedScaleType() != null && getSelectedScaleType().equals(item)) {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), strokeColor);
				} else {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
				}
				holder.button.setImageDrawable(rectContourDrawable);
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		int getItemPosition(GradientScaleType name) {
			return items.indexOf(name);
		}
	}

	private static class TrackColoringViewHolder extends RecyclerView.ViewHolder {

		final TextView colorKey;
		final ImageView icon;
		final ImageView button;

		TrackColoringViewHolder(View itemView) {
			super(itemView);
			colorKey = itemView.findViewById(R.id.groupName);
			icon = itemView.findViewById(R.id.groupIcon);
			button = itemView.findViewById(R.id.outlineRect);
		}
	}
}