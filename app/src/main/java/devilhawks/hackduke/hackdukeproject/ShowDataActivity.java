package devilhawks.hackduke.hackdukeproject;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class ShowDataActivity extends AppCompatActivity {

    private ListView listView;

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

                ArrayList<String> strings = new ArrayList<>();
                for(File file: files){
                    strings.add(extractFile(file));
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,strings);
                listView.setAdapter(arrayAdapter);

                //WordAdapter wordAdapter = new WordAdapter(this, wordFiles, memoryCache, diskCache);

                //ListView listView = (ListView) findViewById(R.id.list);
                //listView.setAdapter(wordAdapter);
            }
        }
    }

    protected String extractFile(File file){
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

            long second = (duration / 1000) % 60;
            long minute = (duration / (1000 * 60)) % 60;
            long hour = (duration / (1000 * 60 * 60)) % 24;
            String time = String.format("%02d:%02d:%02d:%d\n", hour, minute, second, duration);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);
            int mYear = calendar.get(Calendar.YEAR);
            int mMonth = calendar.get(Calendar.MONTH);
            int mDay = calendar.get(Calendar.DAY_OF_MONTH);
            String formattedDate = String.format("%02d/%02d/%02d\n",mMonth,mDay,mYear);

            return("Blinks: " + blinks + "\n" + "Turns: " + turns + "\n" +
                "Tilts: " + tilts + "\n" + "Pauses: " + pauses + "\n" + "Pause Avg. Time: "
                    + String.format("%.2f",pauseAvg) + "\n" + "Duration: " + time +
                    "Date:" + formattedDate);
        }
        return null;
    }

}
