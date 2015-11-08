package devilhawks.hackduke.hackdukeproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ShowDataActivity extends AppCompatActivity {

    private ListView listView;
    public static float maxBlinkRate = 0;
    public static float maxTurnRate = 0;
    public static float maxTiltRate = 0;
    public static float maxPauseRate = 0;
    public static float maxPauseAvg = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);
        listView = (ListView) findViewById(R.id.data_list);
        loadFiles();
    }

    private void loadFiles() {
        File documentsDirectory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (documentsDirectory != null) {
            File[] files = documentsDirectory.listFiles();

            TextView textView = (TextView) findViewById(R.id.empty_message);
            // If none, display empty message
            if (files == null || files.length == 0) {
                textView.setVisibility(View.VISIBLE);
            } else { // Otherwise, add all files of display images in the list
                textView.setVisibility(View.INVISIBLE);

                ArrayList<Dataset> sets = new ArrayList<>();
                for(File file: files){
                    sets.add(extractFile(file));
                }

                DataSetAdapter dataSetAdapter = new DataSetAdapter(this, sets);
                listView.setAdapter(dataSetAdapter);
            }
        }
    }

    protected Dataset extractFile(File file){
        if(file.exists()) {
            int blinks = 0;
            int turns = 0;
            int tilts = 0;
            int pauses = 0;
            float pauseAvg = 0;
            long date = 0;
            long duration = 0;
            BufferedReader bufferedReader = null;
            StringBuilder stringBuilder = null;
            try {
                bufferedReader = new BufferedReader(new FileReader(file));
                stringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();

                while (line != null) {
                    stringBuilder.append(line);
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (stringBuilder != null) {
                JSONObject jsonObject = null;

                // Load JSON string from file
                try {
                    jsonObject = new JSONObject(stringBuilder.toString());
                    blinks = jsonObject.getInt(FaceTrackerActivity.JSON_BLINKS_KEY);
                    turns = jsonObject.getInt(FaceTrackerActivity.JSON_TURNS_KEY);
                    tilts = jsonObject.getInt(FaceTrackerActivity.JSON_TILTS_KEY);
                    pauses = jsonObject.getInt(FaceTrackerActivity.JSON_PAUSES_KEY);
                    pauseAvg = (float) jsonObject.getDouble(FaceTrackerActivity.JSON_PAUSEAVG_KEY);
                    duration = jsonObject.getLong(FaceTrackerActivity.JSON_DURATION_KEY);
                    date = jsonObject.getLong(FaceTrackerActivity.JSON_DATE_KEY);
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }

            int second = (int) (duration / 1000) % 60;
            int minute = (int)(duration / (1000 * 60)) % 60;
            String time = String.format("%02d:%02d", minute, second);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);
            int mYear = calendar.get(Calendar.YEAR);
            int mMonth = calendar.get(Calendar.MONTH);
            int mDay = calendar.get(Calendar.DAY_OF_MONTH);
            String formattedTime = new SimpleDateFormat("HH:mm:ss").format(new Date(date));
            String formattedDate = String.format("%02d/%02d/%02d ", mMonth, mDay, mYear) + formattedTime + "\n";

            float timeInMinutes = minute + ((float)second)/60;
            float blinkRate = ((float) blinks)/timeInMinutes;
            float turnRate = ((float)turns)/timeInMinutes;
            float tiltRate = ((float)tilts)/timeInMinutes;
            float pauseRate = ((float)pauses)/timeInMinutes;
            if(blinkRate > maxBlinkRate) { maxBlinkRate = blinkRate;}
            if(turnRate > maxTurnRate) {maxTurnRate = turnRate;}
            if(tiltRate > maxTiltRate) {maxTiltRate = tiltRate;}
            if(pauseRate > maxPauseRate) {maxPauseRate = pauseRate;}
            if(pauseAvg > maxPauseAvg) {maxPauseAvg = pauseAvg;}

            return new Dataset(formattedDate, time, blinkRate, turnRate, tiltRate, pauseRate, pauseAvg);
        }
        return null;
    }

    class DataSetAdapter extends BaseAdapter {

        /**
         * Context for this adapter.
         */
        Context context;
        /**
         * Inflater to load views.
         */
        LayoutInflater inflater;
        /**
         * List of files to source word info from.
         */
        ArrayList<Dataset> datasets;

        /**
         * Image size for all character thumbnails (in dp).
         */
        public static final int ITEM_HEIGHT = 60;

        public DataSetAdapter(Context context, ArrayList<Dataset> sets){
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.datasets = sets;
        }

        @Override
        public int getCount() {
            return datasets.size();
        }

        @Override
        public Object getItem(int position) {
            return datasets.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;

            if (convertView == null) {  // if it's not recycled, initialize some attributes
                view = inflater.inflate(R.layout.content_dataset,parent,false);
                holder = new ViewHolder();

                holder.textView = (TextView) view.findViewById(R.id.date);
                holder.canvasView = (CanvasView) view.findViewById(R.id.canvas_view);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            // Clear lists on view
            Dataset set = datasets.get(position);
            holder.textView.setText("Date: " + set.formattedDate + " " + "Duration: " + set.formattedDuration);
            holder.canvasView.clearCanvas();
            holder.canvasView.setDataset(set);
            return view;
        }
    }

    /**
     * Class used to hold references to the view components for quick access
     */
    private class ViewHolder{

        public TextView textView;
        public CanvasView canvasView;
    }
}
