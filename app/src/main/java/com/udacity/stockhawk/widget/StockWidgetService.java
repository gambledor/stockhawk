package com.udacity.stockhawk.widget;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.ui.DetailActivity;
import com.udacity.stockhawk.util.Formatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by giuseppelobrutto on 29/04/17.
 */
public class StockWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockWidgetViewFactory(getApplicationContext());
    }

    private class StockWidgetViewFactory implements RemoteViewsFactory {

        private final Context mContext;
        private final List<ContentValues> contentValues;
        private final String TAG = StockWidgetViewFactory.class.getName();

        /**
         * Constructor
         *
         * @param context The application context
         */
        public StockWidgetViewFactory(Context context) {
            mContext = context;
            contentValues = new ArrayList<>();
        }

        @Override
        public void onCreate() {
            loadData();
        }

        @Override
        public void onDataSetChanged() {
            loadData();
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            return contentValues.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            ContentValues cvs = contentValues.get(position);
            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);

            String symbol = cvs.getAsString(Contract.Quote.COLUMN_SYMBOL);
            remoteViews.setTextViewText(R.id.symbol, cvs.getAsString(Contract.Quote.COLUMN_SYMBOL));

            String price = Formatter.dollarFormat.format(cvs.getAsFloat(Contract.Quote.COLUMN_PRICE));
            remoteViews.setTextViewText(R.id.price, price);

            float absoluteChange = cvs.getAsFloat(Contract.Quote.COLUMN_ABSOLUTE_CHANGE);
            if (absoluteChange < 0) {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            } else {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            }

            float percentageChange = cvs.getAsFloat(Contract.Quote.COLUMN_PERCENTAGE_CHANGE);
            remoteViews.setTextViewText(R.id.change, Formatter.percentageFormat.format(percentageChange/100));

            Intent fillInIntent = new Intent(mContext, DetailActivity.class);
            fillInIntent.putExtra(DetailActivity.STOCK_SYMBOL, symbol);
            remoteViews.setOnClickFillInIntent(R.id.list_item, fillInIntent);

            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        private void loadData() {
            contentValues.clear();
            long identity = Binder.clearCallingIdentity();

            try {
                ContentResolver contentResolver = mContext.getContentResolver();
                Cursor cursor = contentResolver.query(
                        Contract.Quote.URI,
                        null,
                        null,
                        null,
                        Contract.Quote.COLUMN_SYMBOL
                );

                String symbol;
                float price,
                        absoluteChange,
                        percentageChange;

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        symbol = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
                        price = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
                        absoluteChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
                        percentageChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE));

                        ContentValues cvs = new ContentValues();
                        cvs.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                        cvs.put(Contract.Quote.COLUMN_PRICE, price);
                        cvs.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, absoluteChange);
                        cvs.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentageChange);

                        contentValues.add(cvs);
                    }

                    cursor.close();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }
}
