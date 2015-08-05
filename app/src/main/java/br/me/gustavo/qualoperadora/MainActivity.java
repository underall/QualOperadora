package br.me.gustavo.qualoperadora;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends ActionBarActivity {
    private ImageView imOperadora;
    private EditText etTelefone;
    private TextView tvResult;
    protected String urlOperadora = "http://consultanumero.info/consulta";
    protected String urlOperadoraImg = "";
    private String result = "";
    private Bitmap bOperadoraImg = null;
    private Boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imOperadora = (ImageView) findViewById(R.id.image_operadora);
        etTelefone = (EditText) findViewById(R.id.edit_telefone);
        tvResult = (TextView) findViewById(R.id.text_result);

        tvResult.setText(urlOperadora);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void submit(View v) {
//        tvResult.setText("Telefonica GSM \n-\n Rio de Janeiro");
        wget(v);
        // Dead loop to wait for wget thread to finish
        while ( !finished );
        tvResult.setText(result);
        Toast.makeText(getBaseContext(), tvResult.getText(), Toast.LENGTH_LONG).show();
        imOperadora.setImageBitmap(bOperadoraImg);
        finished=false;
    }


    void wget(View v)  {
        String TAG = "WGET";
        final String telefone = etTelefone.getText().toString();

        new Thread() {
            String TAG = "WGET.thread()";
            String params = "tel=" + telefone;
//            String params = "tel=21986628155";

            public void run() {
                try {
                    result = "";
                    finished=false;

                    URL url = new URL(urlOperadora);
                    // Configuring POST action to the site
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    // Setting POST method
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);

                    // User Agent and Accept Encoding MUST be set to this works
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux i586; rv:31.0) Gecko/20100101 Firefox/31.0");

                    //Content-type MUST be set for POST data
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    //Setting length for POST data
                    conn.setFixedLengthStreamingMode(params.getBytes().length);

                    //POSTing ???
                    PrintWriter out = new PrintWriter(conn.getOutputStream());
                    out.print(params);
                    out.close();

                    // Making connection
                    conn.connect();

                    //Configuring fetch return
                    InputStream stream = conn.getInputStream();

                    // Cursor for reading return data
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                    String line;
                    int div = 0;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("resultado")) {
                            div = 1;
                            result += line.trim();
                            continue;
                        }
                        if (div > 0) {
                            if (line.contains("<img")) {
                                urlOperadoraImg = line.substring(line.lastIndexOf("=", line.lastIndexOf(".png")) + 2, line.lastIndexOf(".png"));
                                urlOperadoraImg = urlOperadora.substring(0, urlOperadora.lastIndexOf("/")) + urlOperadoraImg + ".png";

                                line = line.substring(line.lastIndexOf("/", line.lastIndexOf(".png")) + 1, line.lastIndexOf(".png")).toUpperCase();
                            }

                            result += line.trim();

                            if (line.contains("<div")) div++;
                            if (line.contains("</div")) div--;
                        }
                    }
                } catch (MalformedURLException e) {
                    Log.d(TAG, "MalformedURLException: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "IOException: " + e.getMessage());
                } catch (NullPointerException e) {
                    Log.d(TAG, "NullPointerException: " + e.getMessage());
                }

                //Debugging output of retrieving data
//                Log.d(TAG, result);
                Log.d(TAG, urlOperadoraImg);

                try {
                    bOperadoraImg = BitmapFactory.decodeStream((InputStream) new URL(urlOperadoraImg).getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                result = Html.fromHtml(result, null, null).toString();

                result = result.trim();
                result = result.replace("\n\n", "\n");

                // Debuggin output of converted data
//                Log.d(TAG, result);

                //Stripping out the image char
//                result = result.substring(result.indexOf("»"));

                // Debuggin after stripping image off
                Log.d(TAG, result);

                // Thread finished
                finished = true;
            }
        }.start();
    }
}